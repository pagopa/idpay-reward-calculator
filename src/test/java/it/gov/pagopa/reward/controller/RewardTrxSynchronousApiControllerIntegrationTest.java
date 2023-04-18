package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import it.gov.pagopa.reward.utils.RewardConstants;
import it.gov.pagopa.reward.utils.Utils;
import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.reward=WARN",
                "logging.level.it.gov.pagopa.reward.exception.ErrorManager=INFO",
        })
class RewardTrxSynchronousApiControllerIntegrationTest  extends BaseIntegrationTest {
    public static final String INITIATIVEID = "INITIATIVEID";
    public static final String USERID = "USERID";
    public static final String TRXIDPROCESSED = "TRXIDPROCESSED";
    private static final int parallelism = 8;
    private static final ExecutorService executor = Executors.newFixedThreadPool(parallelism);

    @Autowired
    private UserInitiativeCountersRepository userInitiativeCountersRepository;

    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    private TransactionProcessedRepository transactionProcessedRepository;

    @SpyBean
    protected RewardContextHolderService rewardContextHolderService;

    @Value("${app.synchronousTransactions.throttlingSeconds}")
    private int throttlingSeconds;

    private final List<FailableConsumer<Integer, Exception>> useCases = new ArrayList<>();

    private final BigDecimal beneficiaryBudget = BigDecimal.valueOf(10_000,2);

    @Test
    void test(){

        int N = Math.max(useCases.size(), 50);

        publishRewardRules();

        List<? extends Future<?>> tasks = IntStream.range(0, N)
                .mapToObj(i -> executor.submit(() -> {
                    try {
                        useCases.get(i % useCases.size()).accept(i);
                    }catch (Exception e) {
                        throw new IllegalStateException("Unexpected exception thrown during test", e);
                    }
                }))
                .toList();

        for (int i = 0; i < tasks.size(); i++) {
            try {
                tasks.get(i).get();
            } catch (Exception e) {
                System.err.printf("UseCase %d (bias %d) failed %n", i % useCases.size(), i);
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (e.getCause() instanceof AssertionFailedError assertionFailedError) {
                    throw assertionFailedError;
                }
                Assertions.fail(e);
            }
        }

    }

    private void publishRewardRules() {
        InitiativeReward2BuildDTO rule = InitiativeReward2BuildDTOFaker.mockInstanceBuilder(0, Collections.emptySet(), RewardValueDTO.class)
                .initiativeId(INITIATIVEID)
                .initiativeName("NAME_" + INITIATIVEID)
                .organizationId("ORGANIZATIONID_" + INITIATIVEID)
                .trxRule(InitiativeTrxConditions.builder()
                        .threshold(ThresholdDTO.builder()
                                .from(BigDecimal.valueOf(5))
                                .fromIncluded(true)
                                .build())
                        .build())
                .rewardRule(RewardValueDTO.builder()
                        .rewardValue(BigDecimal.TEN)
                        .build())
                .build();
        rule.getGeneral().setBeneficiaryBudget(beneficiaryBudget);
        publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, rule);
        RewardRuleConsumerConfigTest.waitForKieContainerBuild(1, rewardContextHolderService);
    }

    private WebTestClient.ResponseSpec previewTrx(SynchronousTransactionRequestDTO trxRequest, String initiativeId){
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(trxRequest))
                .exchange();
    }

    private WebTestClient.ResponseSpec authorizeTrx(SynchronousTransactionRequestDTO trxRequest, String initiativeId){
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(trxRequest))
                .exchange();
    }

    {
        // useCase 0: initiative not existent
        useCases.add(i -> {
            // Setting
            String initiativeId = "DUMMYINITIATIVEID";
            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);

            //expected response
            SynchronousTransactionResponseDTO responseExpectedInitiativeNotFound = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .channel(trxRequest.getChannel())
                    .initiativeId(initiativeId)
                    .userId(trxRequest.getUserId())
                    .operationType(trxRequest.getOperationType())
                    .amount(trxRequest.getAmountCents())
                    .effectiveAmount(Utils.centsToEuro(trxRequest.getAmountCents()))
                    .status(RewardConstants.REWARD_STATE_REJECTED)
                    .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND))
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> previewResponseInitiativeNotFound = extractResponse(previewTrx(trxRequest, initiativeId), HttpStatus.NOT_FOUND, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(previewResponseInitiativeNotFound);
            previewResponseInitiativeNotFound.isEqualTo(responseExpectedInitiativeNotFound);

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> authorizeResponseInitiativeNotFound = extractResponse(authorizeTrx(trxRequest, initiativeId), HttpStatus.NOT_FOUND, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(authorizeResponseInitiativeNotFound);
            authorizeResponseInitiativeNotFound.isEqualTo(responseExpectedInitiativeNotFound);
        });

        // useCase 1: user not onboarded
        useCases.add(i -> {
            // Setting
            String userIdNotOnboarded = "DUMMYUSERID";
            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setUserId(userIdNotOnboarded);

            // Expected response
            SynchronousTransactionResponseDTO responseExpectedInitiativeNotFound = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .channel(trxRequest.getChannel())
                    .initiativeId(INITIATIVEID)
                    .userId(trxRequest.getUserId())
                    .operationType(trxRequest.getOperationType())
                    .amount(trxRequest.getAmountCents())
                    .effectiveAmount(Utils.centsToEuro(trxRequest.getAmountCents()))
                    .status(RewardConstants.REWARD_STATE_REJECTED)
                    .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> previewResponseNotOnboarded = extractResponse(previewTrx(trxRequest, INITIATIVEID), HttpStatus.FORBIDDEN, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(previewResponseNotOnboarded);
            previewResponseNotOnboarded.isEqualTo(responseExpectedInitiativeNotFound);

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> authorizeResponseNotOnboarded = extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.FORBIDDEN, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(authorizeResponseNotOnboarded);
            authorizeResponseNotOnboarded.isEqualTo(responseExpectedInitiativeNotFound);
        });

        // UseCase 2: transaction already processed
        useCases.add(i -> {
            // Setting
            String trxId = TRXIDPROCESSED+i;
            String userId = USERID+i;

            addedPaymentInstrument(1, userId, INITIATIVEID);

            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setTransactionId(trxId);
            trxRequest.setChannel("SYNCPAYMENTCHANNEL");
            trxRequest.setUserId(userId);
            trxRequest.setAmountCents(1_000L);

            Reward reward = Reward.builder()
                    .initiativeId(INITIATIVEID)
                    .organizationId("ORGANIZATIONID_"+INITIATIVEID)
                    .providedReward(BigDecimal.valueOf(100,2))
                    .accruedReward(BigDecimal.valueOf(100,2))
                    .capped(false)
                    .dailyCapped(false)
                    .monthlyCapped(false)
                    .yearlyCapped(false)
                    .weeklyCapped(false)
                    .refund(false)
                    .completeRefund(false)
                    .build();
            TransactionProcessed transactionProcessed =  TransactionProcessed.builder()
                    .id(trxId)
                    .channel("SYNCPAYMENTCHANNEL")
                    .operationTypeTranscoded(trxRequest.getOperationType())
                    .amountCents(trxRequest.getAmountCents())
                    .amount(BigDecimal.ONE)
                    .effectiveAmount(BigDecimal.ONE)
                    .userId(userId)
                    .status(RewardConstants.REWARD_STATE_REWARDED)
                    .rewards(Map.of(INITIATIVEID, reward))
                    .build();

            transactionProcessedRepository.save(transactionProcessed).block();


            // Expected Response
            SynchronousTransactionResponseDTO responseExpectedTrxAlreadyProcessed = SynchronousTransactionResponseDTO.builder()
                    .transactionId(transactionProcessed.getId())
                    .channel(trxRequest.getChannel())
                    .initiativeId(INITIATIVEID)
                    .userId(transactionProcessed.getUserId())
                    .operationType(trxRequest.getOperationType())
                    .amount(trxRequest.getAmountCents())
                    .effectiveAmount(Utils.centsToEuro(trxRequest.getAmountCents()))
                    .status(transactionProcessed.getStatus())
                    .reward(transactionProcessed.getRewards().get(INITIATIVEID))
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> prevResponseAlreadyProcessed = extractResponse(previewTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(prevResponseAlreadyProcessed);
            prevResponseAlreadyProcessed.isEqualTo(responseExpectedTrxAlreadyProcessed);


            SynchronousTransactionResponseDTO responseAuthExpectedTrxAlreadyProcessed = SynchronousTransactionResponseDTO.builder()
                    .transactionId(transactionProcessed.getId())
                    .channel(transactionProcessed.getChannel())
                    .initiativeId(INITIATIVEID)
                    .userId(transactionProcessed.getUserId())
                    .operationType(transactionProcessed.getOperationTypeTranscoded())
                    .amount(transactionProcessed.getAmountCents())
                    .effectiveAmount(transactionProcessed.getEffectiveAmount())
                    .status(transactionProcessed.getStatus())
                    .reward(transactionProcessed.getRewards().get(INITIATIVEID))
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> autResponseAlreadyProcessed = extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.CONFLICT, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(autResponseAlreadyProcessed);
            autResponseAlreadyProcessed.isEqualTo(responseAuthExpectedTrxAlreadyProcessed);
        });

        //UseCase 3: To many request userId-initiativeId (429)
        useCases.add(i -> {
            String userId = "USERTHROTTLED_"+i;

            addedPaymentInstrument(1, userId, INITIATIVEID);

            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setUserId(userId);

            SynchronousTransactionRequestDTO trxRequestThrottled = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequestThrottled.setTransactionId("THROTTLEDTRXID%d".formatted(i));
            trxRequestThrottled.setUserId(userId);

            extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.OK, null);
            extractResponse(authorizeTrx(trxRequestThrottled, INITIATIVEID), HttpStatus.TOO_MANY_REQUESTS, null);


            wait(throttlingSeconds, TimeUnit.SECONDS);
            SynchronousTransactionRequestDTO trxRequestThrottledPassed = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequestThrottledPassed.setTransactionId("THROTTLEDPASSEDTRXID%d".formatted(i));
            trxRequestThrottledPassed.setUserId(userId);
            extractResponse(authorizeTrx(trxRequestThrottled, INITIATIVEID), HttpStatus.OK, null);

        });

        //  UseCase 4: not rewarded
        useCases.add(i -> {
            String userId = USERID+i;

            addedPaymentInstrument(i, userId, INITIATIVEID);

            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setUserId(userId);
            trxRequest.setAmountCents(100L);

            // Expected response
            SynchronousTransactionResponseDTO responseExpectedTrxRejectedRule = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .channel(trxRequest.getChannel())
                    .initiativeId(INITIATIVEID)
                    .userId(trxRequest.getUserId())
                    .operationType(trxRequest.getOperationType())
                    .amount(trxRequest.getAmountCents())
                    .effectiveAmount(Utils.centsToEuro(trxRequest.getAmountCents()))
                    .status(RewardConstants.REWARD_STATE_REJECTED)
                    .rejectionReasons(List.of("TRX_RULE_THRESHOLD_FAIL"))
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> previewResponseRejected = extractResponse(previewTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(previewResponseRejected);
            previewResponseRejected.isEqualTo(responseExpectedTrxRejectedRule);

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> authorizeResponseRejected = extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(authorizeResponseRejected);
            authorizeResponseRejected.isEqualTo(responseExpectedTrxRejectedRule);
        });

        //useCase 5: rewarded
        useCases.add(i -> {
            //Settings
            String userId = USERID+i;
            addedPaymentInstrument(i, userId, INITIATIVEID);

            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setUserId(userId);
            trxRequest.setAmountCents(20000L);


            //Response
            Reward rewardExpected = Reward.builder()
                    .initiativeId(INITIATIVEID)
                    .organizationId("ORGANIZATIONID_"+INITIATIVEID)
                    .providedReward(Utils.centsToEuro(2000L))
                    .accruedReward(Utils.centsToEuro(2000L))
                    .capped(false)
                    .dailyCapped(false)
                    .monthlyCapped(false)
                    .yearlyCapped(false)
                    .weeklyCapped(false)
                    .refund(false)
                    .completeRefund(false)
                    .build();
            SynchronousTransactionResponseDTO responseExpectedTrxRewardRule = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .channel(trxRequest.getChannel())
                    .initiativeId(INITIATIVEID)
                    .operationType(trxRequest.getOperationType())
                    .amount(trxRequest.getAmountCents())
                    .effectiveAmount(Utils.centsToEuro(trxRequest.getAmountCents()))
                    .userId(trxRequest.getUserId())
                    .status(RewardConstants.REWARD_STATE_REWARDED)
                    .reward(rewardExpected)
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> previewResponseRewardRule= extractResponse(previewTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(previewResponseRewardRule);
            previewResponseRewardRule.isEqualTo(responseExpectedTrxRewardRule);

            RewardCounters rewardCounters = RewardCounters.builder()
                    .exhaustedBudget(false)
                    .initiativeBudget(beneficiaryBudget).build();
            rewardExpected.setCounters(rewardCounters);
            responseExpectedTrxRewardRule.setReward(rewardExpected);


            Reward rewardAuthExpected = Reward.builder()
                    .initiativeId(INITIATIVEID)
                    .organizationId("ORGANIZATIONID_"+INITIATIVEID)
                    .providedReward(Utils.centsToEuro(2000L))
                    .accruedReward(Utils.centsToEuro(2000L))
                    .capped(false)
                    .dailyCapped(false)
                    .monthlyCapped(false)
                    .yearlyCapped(false)
                    .weeklyCapped(false)
                    .refund(false)
                    .completeRefund(false)
                    .counters(rewardCounters)
                    .build();
            SynchronousTransactionResponseDTO responseAuthExpectedTrxRewardRule = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .channel(trxRequest.getChannel())
                    .initiativeId(INITIATIVEID)
                    .operationType(trxRequest.getOperationType())
                    .amount(trxRequest.getAmountCents())
                    .effectiveAmount(Utils.centsToEuro(trxRequest.getAmountCents()))
                    .userId(trxRequest.getUserId())
                    .status(RewardConstants.REWARD_STATE_REWARDED)
                    .reward(rewardAuthExpected)
                    .build();

            System.out.println("expected" + responseAuthExpectedTrxRewardRule);


            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> authorizeResponseRewardRule = extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(authorizeResponseRewardRule);
            authorizeResponseRewardRule.value(response -> {
                Assertions.assertEquals(trxRequest.getTransactionId(),response.getTransactionId());
                Assertions.assertEquals(trxRequest.getChannel(),response.getChannel());
                Assertions.assertEquals(INITIATIVEID, response.getInitiativeId());
                Assertions.assertEquals(trxRequest.getUserId(), response.getUserId()); //TODO add other fields
            });
//            authorizeResponseRewardRule.isEqualTo(responseAuthExpectedTrxRewardRule);
        });

        // useCase 6: Budget exhausted
        useCases.add(i -> {
            String userId = USERID+i;

            //Settings
            addedPaymentInstrument(i, userId, INITIATIVEID);

            UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, INITIATIVEID);
            userInitiativeCounters.setUpdateDate(LocalDateTime.now().minusDays(1));
            userInitiativeCounters.setExhaustedBudget(true);
            userInitiativeCountersRepository.save(userInitiativeCounters).block();

            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setUserId(userId);
            trxRequest.setAmountCents(20000L);


            //expected response
            SynchronousTransactionResponseDTO responseExpectedBudgetExhausted = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .channel(trxRequest.getChannel())
                    .initiativeId(INITIATIVEID)
                    .userId(trxRequest.getUserId())
                    .operationType(trxRequest.getOperationType())
                    .amount(trxRequest.getAmountCents())
                    .effectiveAmount(Utils.centsToEuro(trxRequest.getAmountCents()))
                    .status(RewardConstants.REWARD_STATE_REJECTED)
                    .rejectionReasons(List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED, RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> previewResponseBudgetExhausted = extractResponse(previewTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(previewResponseBudgetExhausted);
            previewResponseBudgetExhausted.isEqualTo(responseExpectedBudgetExhausted);

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> authorizeResponseBudgetExhausted = extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(authorizeResponseBudgetExhausted);
            authorizeResponseBudgetExhausted.isEqualTo(responseExpectedBudgetExhausted);
        });
    }

    private void addedPaymentInstrument(Integer bias, String userId, String initiativeId) {
        PaymentMethodInfoDTO infoHpan = PaymentMethodInfoDTO.builder()
                .hpan("IDPAY_"+ userId)
                .maskedPan("MASKEDPAN_%d".formatted(bias))
                .brandLogo("BRANDLOGO_%d".formatted(bias)).build();

        HpanInitiativeBulkDTO hpanInitiativeBulkDTO = HpanInitiativeBulkDTO.builder()
                .infoList(List.of(infoHpan))
                .userId(userId)
                .initiativeId(initiativeId)
                .operationDate(LocalDateTime.now())
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .channel(HpanInitiativeConstants.CHANEL_PAYMENT_MANAGER)
                .build();


        Long hpanInitiativeDB = hpanInitiativesRepository.count().block();
        long[] countSaved={0};
        publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer, null, userId, TestUtils.jsonSerializer(hpanInitiativeBulkDTO));
        waitFor(()->(countSaved[0]=hpanInitiativesRepository.count().block()) >= hpanInitiativeDB+1, ()->"Expected %d saved initiatives, read %d".formatted(hpanInitiativeDB+1, countSaved[0]), 60, 1000);

    }

    private <T> WebTestClient.BodySpec<T, ?> extractResponse(WebTestClient.ResponseSpec response, HttpStatus expectedHttpStatus, Class<T> expectedBodyClass){
        response = response.expectStatus().value(httpStatus -> Assertions.assertEquals(expectedHttpStatus.value(), httpStatus));
        if (expectedBodyClass != null){
            return response.expectBody(expectedBodyClass);
        }
        return null;
    }


}