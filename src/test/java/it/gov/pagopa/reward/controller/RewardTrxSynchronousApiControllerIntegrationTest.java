package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.reward=WARN",
                "logging.level.it.gov.pagopa.common=WARN",
                "logging.level.it.gov.pagopa.reward.exception.ErrorManager=INFO",
        })
class RewardTrxSynchronousApiControllerIntegrationTest  extends BaseIntegrationTest {
    public static final String INITIATIVEID = "INITIATIVEID";
    public static final String USERID = "USERID";
    public static final String TRXIDPROCESSED = "TRXIDPROCESSED";
    private static final int parallelism = 8;
    private static final ExecutorService executor = Executors.newFixedThreadPool(parallelism);
    private static final int N = 100;

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

    @Test
    void test(){

        publishRewardRules();

        initializeSettings();

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

    protected WebTestClient.ResponseSpec previewTrx(SynchronousTransactionRequestDTO trxRequest, String initiativeId){
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(trxRequest))
                .exchange();
    }

    protected WebTestClient.ResponseSpec authorizeTrx(SynchronousTransactionRequestDTO trxRequest, String initiativeId){
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(trxRequest))
                .exchange();
    }

    {
        //TODO riutilizzo useCase and erroUseCase delle code
        // useCase 0: initiative not existent
        useCases.add(i -> {
            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            String initiativeId = "DUMMYINITIATIVEID";

            SynchronousTransactionResponseDTO responseExpectedInitiativeNotFound = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .initiativeId(initiativeId)
                    .userId(trxRequest.getUserId())
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
            String userIdNotOnboarded = "DUMMYUSERID";
            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setUserId(userIdNotOnboarded);

            SynchronousTransactionResponseDTO responseExpectedInitiativeNotFound = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .initiativeId(INITIATIVEID)
                    .userId(trxRequest.getUserId())
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
            String trxId = TRXIDPROCESSED+i;
            String userId = USERID+i;
            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setTransactionId(trxId);
            trxRequest.setUserId(userId);

            Reward reward = Reward.builder()
                    .initiativeId(INITIATIVEID)
                    .providedReward(BigDecimal.ONE)
                    .build();

            TransactionProcessed transactionProcessed =  TransactionProcessed.builder()
                    .id(trxId) //TODO add more fields
                    .userId(userId)
                    .status(RewardConstants.REWARD_STATE_REWARDED)
                    .rewards(Map.of(INITIATIVEID, reward))
                    .build();

            transactionProcessedRepository.save(transactionProcessed).block();

            SynchronousTransactionResponseDTO responseExpectedTrxAlreadyProcessed = SynchronousTransactionResponseDTO.builder()
                    .transactionId(transactionProcessed.getId())
                    .initiativeId(INITIATIVEID)
                    .userId(transactionProcessed.getUserId())
                    .status(transactionProcessed.getStatus())
                    .reward(transactionProcessed.getRewards().get(INITIATIVEID))
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> autResponseAlreadyProcessed = extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.CONFLICT, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(autResponseAlreadyProcessed);
            autResponseAlreadyProcessed.isEqualTo(responseExpectedTrxAlreadyProcessed);
        });

        //UseCase 3: 429 // TODO case
//        useCases.add(i -> {
//            String userId = "USERTHROTTLED_"+i;
//            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
//            trxRequest.setUserId(userId);
//            // save hpanInitiative
//            HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(1);
//            hpanInitiatives.setHpan("IDPAY_"+userId);
//            hpanInitiatives.setUserId(userId);
//
//            ActiveTimeInterval activeTimeInterval = ActiveTimeInterval.builder()
//                    .startInterval(LocalDateTime.now().minusMonths(1L)).build();
//            OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
//                    .initiativeId(INITIATIVEID)
//                    .activeTimeIntervals(List.of(activeTimeInterval))
//                    .build();
//            hpanInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative));
//            hpanInitiativesRepository.save(hpanInitiatives).block();
//            // save userInitiativeCounter
//            UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, INITIATIVEID);
//            userInitiativeCounters.setUpdateDate(LocalDateTime.now());
//            userInitiativeCountersRepository.save(userInitiativeCounters);
//
//            extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.TOO_MANY_REQUESTS, null);
//
//        });

//        UseCase 4: not rewarded
        useCases.add(i -> {
            String userId = USERID+i;
            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setUserId(USERID);
            trxRequest.setAmountCents(100L);
            // Settings
            //Settings
            HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(i);
            hpanInitiatives.setHpan("IDPAY_"+userId);
            hpanInitiatives.setUserId(userId);

            ActiveTimeInterval activeTimeInterval = ActiveTimeInterval.builder()
                    .startInterval(LocalDateTime.now().minusMonths(1L)).build();
            OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                    .initiativeId(INITIATIVEID)
                    .activeTimeIntervals(List.of(activeTimeInterval))
                    .build();
            hpanInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative));

            hpanInitiativesRepository.save(hpanInitiatives).block();

            SynchronousTransactionResponseDTO responseExpectedTrxRejectedRule = SynchronousTransactionResponseDTO.builder()
                    .transactionId(trxRequest.getTransactionId())
                    .initiativeId(INITIATIVEID)
                    .userId(trxRequest.getUserId())
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
            String userId = USERID+i;
            SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(i);
            trxRequest.setUserId(userId);
            trxRequest.setAmountCents(20000L);

            //Settings
            HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(i);
            hpanInitiatives.setHpan("IDPAY_"+userId);
            hpanInitiatives.setUserId(userId);

            ActiveTimeInterval activeTimeInterval = ActiveTimeInterval.builder()
                    .startInterval(LocalDateTime.now().minusMonths(1L)).build();
            OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                    .initiativeId(INITIATIVEID)
                    .activeTimeIntervals(List.of(activeTimeInterval))
                    .build();
            hpanInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative));

            hpanInitiativesRepository.save(hpanInitiatives).block();
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
                    .initiativeId(INITIATIVEID)
                    .userId(trxRequest.getUserId())
                    .status(RewardConstants.REWARD_STATE_REWARDED)
                    .reward(rewardExpected)
                    .build();

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> previewResponseRewardRule= extractResponse(previewTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(previewResponseRewardRule);
            previewResponseRewardRule.isEqualTo(responseExpectedTrxRewardRule);

            WebTestClient.BodySpec<SynchronousTransactionResponseDTO, ?> authorizeResponseRewardRule = extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.OK, SynchronousTransactionResponseDTO.class);
            Assertions.assertNotNull(authorizeResponseRewardRule);
            authorizeResponseRewardRule.value(response -> {
               Assertions.assertEquals(trxRequest.getTransactionId(), response.getTransactionId()); //TODO aggiungere altre assertions, rendendole comune col caso rejected
            });

        });
    }

    private <T> WebTestClient.BodySpec<T, ?> extractResponse(WebTestClient.ResponseSpec response, HttpStatus expectedHttpStatus, Class<T> expectedBodyClass){
        response.expectStatus().value(httpStatus -> Assertions.assertEquals(expectedHttpStatus.value(), httpStatus));
        if (expectedBodyClass != null){
            return response.expectBody(expectedBodyClass);
        }
        return null;
    }

    private void initializeSettings() {
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(1);
        hpanInitiatives.setHpan("IDPAY_"+USERID);
        hpanInitiatives.setUserId(USERID);

        ActiveTimeInterval activeTimeInterval = ActiveTimeInterval.builder()
                .startInterval(LocalDateTime.now().minusMonths(1L)).build();
        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(INITIATIVEID)
                .activeTimeIntervals(List.of(activeTimeInterval))
                .build();
        hpanInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative));

        hpanInitiativesRepository.save(hpanInitiatives).block(); //TODO check if present into DB
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
        publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, rule);
        RewardRuleConsumerConfigTest.waitForKieContainerBuild(1, rewardContextHolderService);
    }
}