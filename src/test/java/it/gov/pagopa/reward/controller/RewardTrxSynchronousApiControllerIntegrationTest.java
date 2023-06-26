package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ErrorManager;
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
import it.gov.pagopa.reward.enums.InitiativeRewardType;
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
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.reward=WARN",
                "logging.level.it.gov.pagopa.common.web.exception.ErrorManager=OFF",
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

    @SpyBean
    private UserInitiativeCountersRepository userInitiativeCountersRepositorySpy;
    @Value("${app.trx-retries.counters-update.retries}")
    private int maxRetries;

    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    private TransactionProcessedRepository transactionProcessedRepository;

    @SpyBean
    protected RewardContextHolderService rewardContextHolderService;

    @SpyBean
    protected ErrorManager errorManagerSpy;

    @Value("${app.synchronousTransactions.throttlingSeconds}")
    private int throttlingSeconds;

    private final List<FailableConsumer<Integer, Exception>> useCases = new ArrayList<>();

    private final Map<String, AtomicInteger> userId2ConfiguredFailingAttemptsCountdown = new ConcurrentHashMap<>();

    @Test
    void test() {

        int N = Math.max(useCases.size(), 50);

        publishRewardRules();

        configureSpies();

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
                Mockito.mockingDetails(errorManagerSpy).getInvocations()
                        .stream()
                        .filter(ex->!ex.getArgument(0).getClass().equals(RuntimeException.class))
                        .forEach(ex -> System.err.println("ErrorManager invocation: " + ex));
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (e.getCause() instanceof AssertionFailedError assertionFailedError) {
                    throw assertionFailedError;
                }
                Assertions.fail(e);
            }
        }
    }

    private void configureSpies() {
        // configuring userInitiativeCounterSpy 2 fail during saveAll
        Mockito.doAnswer(i -> {
                    @SuppressWarnings("unchecked")
                    Iterable<UserInitiativeCounters> ctrs = i.getArgument(0, Iterable.class);
                    UserInitiativeCounters ctr = ctrs.iterator().next();
                    AtomicInteger countdown = userId2ConfiguredFailingAttemptsCountdown.get(ctr.getUserId());

                    return Flux.defer(()-> {
                        if (countdown != null && countdown.decrementAndGet() >= 0) {
                            return Flux.error(new RuntimeException("DUMMY_EXCEPTION_WHEN_STORING_COUNTERS"));
                        } else {
                            try {
                                return userInitiativeCountersRepositorySpy.save(ctr).thenMany(Flux.just(ctr));
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                })
                .when(userInitiativeCountersRepositorySpy)
                .saveAll(Mockito.argThat((Iterable<UserInitiativeCounters> ctrs) ->
                        StreamSupport.stream(ctrs.spliterator(), false)
                                .anyMatch(t -> userId2ConfiguredFailingAttemptsCountdown.get(t.getUserId()) != null
                                )));
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
                .initiativeRewardType(InitiativeRewardType.DISCOUNT)
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

        // throttling
        TestUtils.wait(500, TimeUnit.MILLISECONDS);
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
            userInitiativeCountersRepositorySpy.save(userInitiativeCounters).block();

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

            SynchronousTransactionResponseDTO cancelResponse1 = assertCancel(authorizeResponse, HttpStatus.OK);

            waitThrottling();

            SynchronousTransactionResponseDTO cancelResponse2 = assertCancel(authorizeResponse, HttpStatus.CONFLICT);
            Assertions.assertEquals(cancelResponse1, cancelResponse2);
        });

        // useCase 7: cancelling REJECTED authorize
        useCases.add(i -> {
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            trxRequest.setAmountCents(1_00L);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REJECTED, List.of("TRX_RULE_THRESHOLD_FAIL"), null);

            onboardUser(trxRequest);

            configureUserCounterSpy2ThrowException(trxRequest, maxRetries); // until maxRetries attempts, it will retry the update

            assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);
            waitThrottling();
            extractResponse(cancelTrx(trxRequest.getTransactionId()), HttpStatus.NOT_FOUND, null);
        });

        //useCase 8: handling stuck authorization at first store, second attempt ok
        useCases.add(i -> {
            //Settings
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REWARDED, null, getRewardExpected());

            configureUserCounterSpy2ThrowException(trxRequest, maxRetries+1);

            onboardUser(trxRequest);

            assertPreview(trxRequest, HttpStatus.OK, expectedResponse);
            extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.INTERNAL_SERVER_ERROR, null);

            checkStored(trxRequest.getTransactionId(), expectedResponse.getReward());

            waitThrottling();

            assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);
        });

        //useCase 9: handling stuck cancel at first store, second attempt ok
        useCases.add(i -> {
            //Settings
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REWARDED, null, getRewardExpected());

            onboardUser(trxRequest);
            assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);

            configureUserCounterSpy2ThrowException(trxRequest, maxRetries+1);
            waitThrottling();

            extractResponse(cancelTrx(trxRequest.getTransactionId()), HttpStatus.INTERNAL_SERVER_ERROR, null);

            checkStored(trxRequest.getTransactionId(), expectedResponse.getReward());
            checkStored(trxRequest.getTransactionId() + "_REFUND", getExpectedCancelReward(expectedResponse));

            waitThrottling();

            assertCancel(expectedResponse, HttpStatus.OK);
        });

        // useCase 10: cancelling stuck authorize
        useCases.add(i -> {
            //Settings
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REWARDED, null, getRewardExpected());

            configureUserCounterSpy2ThrowException(trxRequest, maxRetries+1);

            onboardUser(trxRequest);

            extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.INTERNAL_SERVER_ERROR, null);
            checkStored(trxRequest.getTransactionId(), expectedResponse.getReward());

            waitThrottling();

            extractResponse(cancelTrx(trxRequest.getTransactionId()), HttpStatus.NOT_FOUND, null);

            UserInitiativeCounters expectedCounter = new UserInitiativeCounters(trxRequest.getUserId(), INITIATIVEID);
            expectedCounter.setUpdateDate(expectedCounter.getUpdateDate().truncatedTo(ChronoUnit.MINUTES));

            UserInitiativeCounters counter = userInitiativeCountersRepositorySpy.findById(UserInitiativeCounters.buildId(trxRequest.getUserId(), INITIATIVEID)).block();
            Assertions.assertNotNull(counter);
            counter.setUpdateDate(counter.getUpdateDate().truncatedTo(ChronoUnit.MINUTES));

            Assertions.assertEquals(
                    expectedCounter,
                    counter
            );
        });

        //useCase 11: handling stuck authorization, rollback at successive trx
        useCases.add(i -> {
            //Settings
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REWARDED, null, getRewardExpected());

            configureUserCounterSpy2ThrowException(trxRequest, maxRetries+1);

            onboardUser(trxRequest);

            assertPreview(trxRequest, HttpStatus.OK, expectedResponse);
            extractResponse(authorizeTrx(trxRequest, INITIATIVEID), HttpStatus.INTERNAL_SERVER_ERROR, null);

            checkStored(trxRequest.getTransactionId(), expectedResponse.getReward());

            waitThrottling();

            SynchronousTransactionRequestDTO trxRequest2 = buildTrxRequest(i);
            trxRequest2.setTransactionId(trxRequest.getTransactionId()+"_SUCCESSIVE");
            SynchronousTransactionResponseDTO expectedResponse2 = getExpectedChargeResponse(INITIATIVEID, trxRequest2, RewardConstants.REWARD_STATE_REWARDED, null, getRewardExpected());

            assertAuthorize(trxRequest2, HttpStatus.OK, expectedResponse2);

            Assertions.assertNull(transactionProcessedRepository.findById(trxRequest.getTransactionId()).block());
        });

        //useCase 12: handling stuck cancel, rollback at successive trx
        useCases.add(i -> {
            //Settings
            SynchronousTransactionRequestDTO trxRequest = buildTrxRequest(i);
            SynchronousTransactionResponseDTO expectedResponse = getExpectedChargeResponse(INITIATIVEID, trxRequest, RewardConstants.REWARD_STATE_REWARDED, null, getRewardExpected());
            String refundTrxId = trxRequest.getTransactionId() + "_REFUND";

            onboardUser(trxRequest);
            assertAuthorize(trxRequest, HttpStatus.OK, expectedResponse);

            configureUserCounterSpy2ThrowException(trxRequest, maxRetries+1);
            waitThrottling();

            extractResponse(cancelTrx(trxRequest.getTransactionId()), HttpStatus.INTERNAL_SERVER_ERROR, null);

            checkStored(trxRequest.getTransactionId(), expectedResponse.getReward());
            checkStored(refundTrxId, getExpectedCancelReward(expectedResponse));

            waitThrottling();

            SynchronousTransactionRequestDTO trxRequest2 = buildTrxRequest(i);
            trxRequest2.setTransactionId(trxRequest.getTransactionId()+"_SUCCESSIVE");
            RewardCounters expectedRewardCounter2 = getUpdatedRewardCounters(1, AMOUNT, REWARD, expectedResponse.getReward().getCounters());
            Reward expectedReward2 = getRewardExpected(expectedRewardCounter2);
            SynchronousTransactionResponseDTO expectedResponse2 = getExpectedChargeResponse(INITIATIVEID, trxRequest2, RewardConstants.REWARD_STATE_REWARDED, null, expectedReward2);

            assertAuthorize(trxRequest2, HttpStatus.OK, expectedResponse2);

            Assertions.assertNotNull(transactionProcessedRepository.findById(trxRequest.getTransactionId()).block());
            Assertions.assertNull(transactionProcessedRepository.findById(refundTrxId).block());
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

        assertStoredTransactionProcessed(initiativeId, expectedHttpStatus, expectedResponse);

        return authorizeResponse;
    }

    private void assertStoredTransactionProcessed(String initiativeId, HttpStatus expectedHttpStatus, SynchronousTransactionResponseDTO expectedResponse) {
        BaseTransactionProcessed stored = transactionProcessedRepository.findById(expectedResponse.getTransactionId()).block();
        if(HttpStatus.OK.equals(expectedHttpStatus) || HttpStatus.CONFLICT.equals(expectedHttpStatus)){
            Assertions.assertNotNull(stored);

            if(RewardConstants.REWARD_STATE_REWARDED.equals(expectedResponse.getStatus())) {
                RewardCounters authCounter = expectedResponse.getReward().getCounters();

                UserInitiativeCounters storedCounter = userInitiativeCountersRepositorySpy.findById(UserInitiativeCounters.buildId(expectedResponse.getUserId(), initiativeId)).block();
                Assertions.assertNotNull(storedCounter);

                Assertions.assertEquals(authCounter.getVersion(), storedCounter.getVersion());

                Assertions.assertEquals(authCounter.getTrxNumber(), storedCounter.getTrxNumber());
                Assertions.assertEquals(authCounter.getTotalAmount(), storedCounter.getTotalAmount());
                Assertions.assertEquals(authCounter.getTotalReward(), storedCounter.getTotalReward());

                Assertions.assertEquals(authCounter.isExhaustedBudget(), storedCounter.isExhaustedBudget());

                Assertions.assertEquals(Optional.ofNullable(authCounter.getDailyCounters()).orElse(Collections.emptyMap()), storedCounter.getDailyCounters());
                Assertions.assertEquals(Optional.ofNullable(authCounter.getWeeklyCounters()).orElse(Collections.emptyMap()), storedCounter.getWeeklyCounters());
                Assertions.assertEquals(Optional.ofNullable(authCounter.getMonthlyCounters()).orElse(Collections.emptyMap()), storedCounter.getMonthlyCounters());
                Assertions.assertEquals(Optional.ofNullable(authCounter.getYearlyCounters()).orElse(Collections.emptyMap()), storedCounter.getYearlyCounters());

                Assertions.assertNull(storedCounter.getUpdatingTrxId());
            }
        } else {
            Assertions.assertNull(stored);
        }
    }

    private SynchronousTransactionResponseDTO assertCancel(SynchronousTransactionResponseDTO authorizeResponse, HttpStatus expectedHttpStatus){
        SynchronousTransactionResponseDTO cancelResponse = extractResponse(cancelTrx(authorizeResponse.getTransactionId()), expectedHttpStatus, SynchronousTransactionResponseDTO.class);
        Assertions.assertNotNull(cancelResponse);
        assertionsRefundResponse(authorizeResponse, cancelResponse);

        assertStoredTransactionProcessed(INITIATIVEID, expectedHttpStatus, cancelResponse);

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

        Assertions.assertEquals(getExpectedCancelReward(authResponse),
                refundResponse.getReward());

        TestUtils.checkNotNullFields(refundResponse, "rejectionReasons");
    }

    private Reward getExpectedCancelReward(SynchronousTransactionResponseDTO authResponse) {
        Reward authReward = authResponse.getReward();
        RewardCounters authCounters = authReward.getCounters();
        RewardCounters counters = getUpdatedRewardCounters(-1, authResponse.getEffectiveAmount().negate(), authReward.getAccruedReward().negate(), authCounters);

        return Reward.builder()
                        .initiativeId(authReward.getInitiativeId())
                        .organizationId(authReward.getOrganizationId())
                        .accruedReward(authReward.getAccruedReward().negate())
                        .providedReward(authReward.getAccruedReward().negate())
                        .refund(true)
                        .completeRefund(true)
                        .counters(counters)
                        .build();
    }

    private static RewardCounters getUpdatedRewardCounters(int trxNumberDelta, BigDecimal amount, BigDecimal reward, RewardCounters previousCounter) {
        return previousCounter.toBuilder()
                .version(previousCounter.getVersion()+1)
                .trxNumber(previousCounter.getTrxNumber()+trxNumberDelta)
                .totalAmount(previousCounter.getTotalAmount().add(amount))
                .totalReward(previousCounter.getTotalReward().add(reward))
                .build();
    }

    private void configureUserCounterSpy2ThrowException(SynchronousTransactionRequestDTO trxRequest, int minAttempts) {
        userId2ConfiguredFailingAttemptsCountdown.put(trxRequest.getUserId(), new AtomicInteger(minAttempts));
    }

    private void checkStored(String trxRequest, Reward expectedResponse) {
        BaseTransactionProcessed cancelStored = transactionProcessedRepository.findById(trxRequest).block();
        Assertions.assertNotNull(cancelStored);
        Assertions.assertEquals(expectedResponse, cancelStored.getRewards().get(INITIATIVEID));
    }
}