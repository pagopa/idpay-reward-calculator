package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.connector.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
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
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import it.gov.pagopa.reward.utils.RewardConstants;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.reward=WARN",
                "logging.level.it.gov.pagopa.common.web.exception.ErrorManager=WARN",
                "logging.level.it.gov.pagopa.common.reactive.utils.PerformanceLogger=WARN",
                "logging.level.it.gov.pagopa.reward.service.reward.RewardContextHolderServiceImpl=WARN",
                "logging.level.it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer=WARN",
        })
class RewardTrxSynchronousApiControllerIntegrationTest extends BaseIntegrationTest {

    public static final String INITIATIVEID = "INITIATIVEID";
    private final BigDecimal beneficiaryBudget = BigDecimal.valueOf(10_000, 2);
    public static final long AMOUNT_CENTS = 200_00L;
    public static final BigDecimal AMOUNT = CommonUtilities.centsToEuro(AMOUNT_CENTS);
    public static final BigDecimal REWARD = TestUtils.bigDecimalValue(20);

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

    @Test
    void test() {

        int N = Math.max(useCases.size(), 50);

        publishRewardRules();

        List<? extends Future<?>> tasks = IntStream.range(0, N)
                .mapToObj(i -> executor.submit(() -> {
                    try {
                        useCases.get(i % useCases.size()).accept(i);
                    } catch (Exception e) {
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
        kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, rule);
        RewardRuleConsumerConfigTest.waitForKieContainerBuild(1, rewardContextHolderService);
    }

    //region API invokes
    private WebTestClient.ResponseSpec previewTrx(SynchronousTransactionRequestDTO trxRequest, String initiativeId) {
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/initiative/preview/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(trxRequest))
                .exchange();
    }

    private WebTestClient.ResponseSpec authorizeTrx(SynchronousTransactionRequestDTO trxRequest, String initiativeId) {
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/initiative/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(trxRequest))
                .exchange();
    }

    private WebTestClient.ResponseSpec cancelTrx(String trxId) {
        return webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/reward/{trxId}")
                        .build(trxId))
                .exchange();
    }
//endregion

    //region utility methods
    private void onboardUser(SynchronousTransactionRequestDTO trxRequest) {
        String userId = trxRequest.getUserId();

        PaymentMethodInfoDTO infoHpan = new PaymentMethodInfoDTO();
        infoHpan.setHpan("IDPAY_" + userId);

        HpanInitiativeBulkDTO hpanInitiativeBulkDTO = HpanInitiativeBulkDTO.builder()
                .infoList(List.of(infoHpan))
                .userId(userId)
                .initiativeId(INITIATIVEID)
                .operationDate(LocalDateTime.now())
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .channel(HpanInitiativeConstants.CHANEL_PAYMENT_MANAGER)
                .build();

        kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer, null, userId, TestUtils.jsonSerializer(hpanInitiativeBulkDTO));

        TestUtils.waitFor(() -> hpanInitiativesRepository.findById(infoHpan.getHpan()).block() != null, () -> "IDPAY PaymentMethod not stored for userId %s".formatted(userId), 60, 1000);
    }

    private SynchronousTransactionRequestDTO buildTrxRequest(Integer bias) {
        SynchronousTransactionRequestDTO trxRequest = SynchronousTransactionRequestDTOFaker.mockInstance(bias);
        trxRequest.setTrxDate(OffsetDateTime.now().plusMinutes(1));
        trxRequest.setAmountCents(AMOUNT_CENTS);
        return trxRequest;
    }

    private <T> T extractResponse(WebTestClient.ResponseSpec response, HttpStatus expectedHttpStatus, Class<T> expectedBodyClass) {
        response = response.expectStatus().value(httpStatus -> Assertions.assertEquals(expectedHttpStatus.value(), httpStatus));
        if (expectedBodyClass != null) {
            return response.expectBody(expectedBodyClass).returnResult().getResponseBody();
        }
        return null;
    }

    private void waitThrottling() {
        TestUtils.wait(throttlingSeconds, TimeUnit.SECONDS);
    }
//endregion

    {
        // useCase 0: initiative not existent
        useCases.add(i -> {
            String initiativeId = "DUMMYINITIATIVEID";
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(initiativeId, trxRequest, RewardConstants.REWARD_STATE_REJECTED, List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND), null);

            assertPreview(trxRequest, initiativeId, HttpStatus.NOT_FOUND, expectedResponse);
            assertAuthorize(trxRequest, initiativeId, HttpStatus.NOT_FOUND, expectedResponse);
        });

        // useCase 1: user not onboarded
        useCases.add(i -> {
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REJECTED, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), null);

            assertPreview(trxRequest, HttpStatus.FORBIDDEN, expectedResponse);
            assertAuthorize(trxRequest, HttpStatus.FORBIDDEN, expectedResponse);
        });

        //UseCase 2: To many request userId-initiativeId (429)
        useCases.add(i -> {
            SynchronousTransactionRequestDTO trx1 = buildTrxRequest(i);
            Reward expectedTrx1Reward = getRewardExpected();

            SynchronousTransactionRequestDTO trx2 = buildTrxRequest(i);
            trx2.setTransactionId(trx1.getTransactionId() + "THROTTLEDSUCCESSIVETRX");
            Reward expectedTrx2Reward = expectedTrx1Reward.toBuilder()
                    .counters(getUpdatedRewardCounters(1, AMOUNT, REWARD, expectedTrx1Reward.getCounters()))
                    .build();

            onboardUser(trx1);

            // Authorize trx1
            assertAuthorize(trx1, HttpStatus.OK, getExpectedChargeResponse(INITIATIVEID, trx1, RewardConstants.REWARD_STATE_REWARDED, null, expectedTrx1Reward));
            // Throttling on same counter
            extractResponse(authorizeTrx(trx2, INITIATIVEID), HttpStatus.TOO_MANY_REQUESTS, null);

            waitThrottling();

            // Authorize after throttling period
            assertAuthorize(trx2, HttpStatus.OK, getExpectedChargeResponse(INITIATIVEID, trx2, RewardConstants.REWARD_STATE_REWARDED, null, expectedTrx2Reward));
        });

        //  UseCase 3: not rewarded
        useCases.add(i -> {
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            trxRequest.setAmountCents(1_00L);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REJECTED, List.of("TRX_RULE_THRESHOLD_FAIL"), null);

            onboardUser(trxRequest);

            assertPreview(trxRequest, HttpStatus.OK, expectedResponse);
            assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);
        });

        //useCase 4: rewarded and already processed
        useCases.add(i -> {
            //Settings
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REWARDED, null, getRewardExpected());

            onboardUser(trxRequest);

            assertPreview(trxRequest, HttpStatus.OK, expectedResponse);
            SynchronousTransactionResponseDTO authorizeResponse = assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);

            // case already processed 409, expecting same result
            assertAuthorize(trxRequest, HttpStatus.CONFLICT, authorizeResponse);
        });

        // useCase 5: Budget exhausted
        useCases.add(i -> {
            //Settings
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            onboardUser(trxRequest);

            UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(trxRequest.getUserId(), INITIATIVEID);
            userInitiativeCounters.setUpdateDate(LocalDateTime.now().minusDays(1));
            userInitiativeCounters.setExhaustedBudget(true);
            userInitiativeCountersRepository.save(userInitiativeCounters).block();

            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest,
                    RewardConstants.REWARD_STATE_REJECTED,
                    List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED, RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE),
                    null);

            assertPreview(trxRequest, HttpStatus.OK, expectedResponse);
            assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);
        });

        // useCase 6: Cancel trx
        useCases.add(i -> {
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REWARDED, null, getRewardExpected());

            onboardUser(trxRequest);

            // cancel not existent Trx
            extractResponse(cancelTrx(trxRequest.getTransactionId()), HttpStatus.NOT_FOUND, null);

            // Trx still doesn't exists after preview
            assertPreview(trxRequest, HttpStatus.OK, expectedResponse);
            extractResponse(cancelTrx(trxRequest.getTransactionId()), HttpStatus.NOT_FOUND, null);

            // 429 if cancelling without waiting throttling
            SynchronousTransactionResponseDTO authorizeResponse = assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);
            extractResponse(cancelTrx(trxRequest.getTransactionId()), HttpStatus.TOO_MANY_REQUESTS, null);

            waitThrottling();

            assertCancel(authorizeResponse, HttpStatus.OK);
        });

        // useCase 7: cancelling REJECTED authorize
        useCases.add(i -> {
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            trxRequest.setAmountCents(1_00L);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REJECTED, List.of("TRX_RULE_THRESHOLD_FAIL"), null);

            onboardUser(trxRequest);

            assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);
            waitThrottling();
            extractResponse(cancelTrx(trxRequest.getTransactionId()), HttpStatus.NOT_FOUND, null);
        });
    }

    private SynchronousTransactionResponseDTO assertPreview(SynchronousTransactionRequestDTO trxRequest, HttpStatus expectedStatus, SynchronousTransactionResponseDTO expectedResponse) {
        return assertPreview(trxRequest, INITIATIVEID, expectedStatus, expectedResponse);
    }
    private SynchronousTransactionResponseDTO assertPreview(SynchronousTransactionRequestDTO trxRequest, String initiativeId, HttpStatus expectedHttpStatus, SynchronousTransactionResponseDTO expectedResponse) {
        SynchronousTransactionResponseDTO previewResponse = extractResponse(previewTrx(trxRequest, initiativeId), expectedHttpStatus, SynchronousTransactionResponseDTO.class);
        Assertions.assertNotNull(previewResponse);
        Assertions.assertEquals(expectedResponse, previewResponse);
        return previewResponse;
    }

    private SynchronousTransactionResponseDTO assertAuthorize(SynchronousTransactionRequestDTO trxRequest, HttpStatus expectedStatus, SynchronousTransactionResponseDTO expectedResponse) {
        return assertAuthorize(trxRequest, INITIATIVEID, expectedStatus, expectedResponse);
    }
    private SynchronousTransactionResponseDTO assertAuthorize(SynchronousTransactionRequestDTO trxRequest, String initiativeId, HttpStatus expectedHttpStatus, SynchronousTransactionResponseDTO expectedResponse) {
        SynchronousTransactionResponseDTO authorizeResponse = extractResponse(authorizeTrx(trxRequest, initiativeId), expectedHttpStatus, SynchronousTransactionResponseDTO.class);
        Assertions.assertNotNull(authorizeResponse);
        Assertions.assertEquals(expectedResponse, authorizeResponse);

        BaseTransactionProcessed stored = transactionProcessedRepository.findById(trxRequest.getTransactionId()).block();
        if(HttpStatus.OK.equals(expectedHttpStatus) || HttpStatus.CONFLICT.equals(expectedHttpStatus)){
            Assertions.assertNotNull(stored);
        } else {
            Assertions.assertNull(stored);
        }

        return authorizeResponse;
    }

    private SynchronousTransactionResponseDTO assertCancel(SynchronousTransactionResponseDTO authorizeResponse, HttpStatus expectedHttpStatus){
        SynchronousTransactionResponseDTO cancelResponse = extractResponse(cancelTrx(authorizeResponse.getTransactionId()), expectedHttpStatus, SynchronousTransactionResponseDTO.class);
        Assertions.assertNotNull(cancelResponse);
        assertionsRefundResponse(authorizeResponse, cancelResponse);
        return cancelResponse;
    }

    private Reward getRewardExpected() {
        RewardCounters counters = new RewardCounters();
        counters.setInitiativeBudget(beneficiaryBudget);
        counters.setVersion(1L);
        counters.setTrxNumber(1L);
        counters.setTotalAmount(AMOUNT);
        counters.setTotalReward(REWARD);
        return getRewardExpected(counters);
    }
    private Reward getRewardExpected(RewardCounters counters) {
        return Reward.builder()
                .initiativeId(INITIATIVEID)
                .organizationId("ORGANIZATIONID_" + INITIATIVEID)
                .providedReward(REWARD)
                .accruedReward(REWARD)
                .capped(false)
                .dailyCapped(false)
                .monthlyCapped(false)
                .yearlyCapped(false)
                .weeklyCapped(false)
                .refund(false)
                .completeRefund(false)
                .counters(counters)
                .build();
    }

    private SynchronousTransactionResponseDTO getExpectedChargeResponse(String initiativeId, SynchronousTransactionRequestDTO trxRequest, String status, List<String> rejectionReasons, Reward reward) {
        return SynchronousTransactionResponseDTO.builder()
                .transactionId(trxRequest.getTransactionId())
                .channel(trxRequest.getChannel())
                .initiativeId(initiativeId)
                .userId(trxRequest.getUserId())
                .amountCents(trxRequest.getAmountCents())
                .amount(CommonUtilities.centsToEuro(trxRequest.getAmountCents()))
                .effectiveAmount(CommonUtilities.centsToEuro(trxRequest.getAmountCents()))
                .status(status)
                .rejectionReasons(rejectionReasons)
                .operationType(OperationType.CHARGE)
                .reward(reward)
                .build();
    }

    private void assertionsRefundResponse(SynchronousTransactionResponseDTO authResponse, SynchronousTransactionResponseDTO refundResponse) {
        Assertions.assertEquals(authResponse.getTransactionId() + "_REFUND", refundResponse.getTransactionId());
        Assertions.assertEquals(authResponse.getChannel(), refundResponse.getChannel());
        Assertions.assertEquals(authResponse.getInitiativeId(), refundResponse.getInitiativeId());
        Assertions.assertEquals(OperationType.REFUND, refundResponse.getOperationType());
        Assertions.assertEquals(authResponse.getAmountCents(), refundResponse.getAmountCents());
        Assertions.assertEquals(authResponse.getAmount(), refundResponse.getAmount());
        Assertions.assertEquals(TestUtils.bigDecimalValue(0), refundResponse.getEffectiveAmount());
        Assertions.assertEquals(authResponse.getStatus(), refundResponse.getStatus());
        Assertions.assertEquals(authResponse.getRejectionReasons(), refundResponse.getRejectionReasons());

        Reward authReward = authResponse.getReward();
        RewardCounters authCounters = authReward.getCounters();

        RewardCounters counters = getUpdatedRewardCounters(-1, authResponse.getEffectiveAmount().negate(), authReward.getAccruedReward().negate(), authCounters);

        Assertions.assertEquals(
                Reward.builder()
                        .initiativeId(authReward.getInitiativeId())
                        .organizationId(authReward.getOrganizationId())
                        .accruedReward(authReward.getAccruedReward().negate())
                        .providedReward(authReward.getAccruedReward().negate())
                        .refund(true)
                        .completeRefund(true)
                        .counters(counters)
                        .build(),
                refundResponse.getReward());

        TestUtils.checkNotNullFields(refundResponse, "rejectionReasons");
    }

    private static RewardCounters getUpdatedRewardCounters(int trxNumberDelta, BigDecimal amount, BigDecimal reward, RewardCounters previousCounter) {
        return previousCounter.toBuilder()
                .version(previousCounter.getVersion()+1)
                .trxNumber(previousCounter.getTrxNumber()+trxNumberDelta)
                .totalAmount(previousCounter.getTotalAmount().add(amount))
                .totalReward(previousCounter.getTotalReward().add(reward))
                .build();
    }

}