package it.gov.pagopa.reward.connector.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateOutcomeDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.OnboardingFamilies;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.lookup.HpanUpdateNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.OnboardingFamiliesFaker;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.lookup.HpanInitiativeMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.lookup.HpanInitiativesServiceImpl=DEBUG",
        "logging.level.it.gov.pagopa.reward.service.lookup.ops.AddHpanServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.lookup.ops.DeleteHpanServiceImpl=OFF",
        "logging.level.it.gov.pagopa.common.reactive.utils.PerformanceLogger=WARN",
})
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"})
class HpanInitiaveConsumerConfigTestDeprecated extends BaseIntegrationTest {
    private static final String INITIATIVE_ID_PF = "INITIATIVE_ID_PF";
    private static final String INITIATIVE_ID_NF = "INITIATIVE_ID_NF";

    private final int dbElementsNumbers = 2;
    private final int updatedHpanNumbers = 10;
    private final int minBiasForNFInitiative = dbElementsNumbers + ((updatedHpanNumbers - dbElementsNumbers)/2);

    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    private UserInitiativeCountersRepository userInitiativeCountersRepository;

    @Autowired
    private OnboardingFamiliesRepository onboardingFamiliesRepository;

    @SpyBean
    private HpanUpdateNotifierService hpanUpdateNotifierServiceSpy;

    @SpyBean
    private RewardContextHolderService rewardContextHolderService;

    @AfterEach
    void cleanData(){
        hpanInitiativesRepository.deleteAll().block();
        userInitiativeCountersRepository.deleteAll().block();
    }
    @Test
    void hpanInitiativeConsumer() {
        int notValidMessages = errorUseCases.size();
        int concurrencyMessages = 2;
        long maxWaitingMs = 30000;

        initializeDB(dbElementsNumbers);
        initializeRules();

        List<String> hpanUpdatedEvents = new ArrayList<>(buildValidPayloads(0,dbElementsNumbers));
        hpanUpdatedEvents.addAll(IntStream.range(0, notValidMessages).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        hpanUpdatedEvents.addAll(buildValidPayloads(dbElementsNumbers,updatedHpanNumbers));
        hpanUpdatedEvents.addAll(buildConcurrencyMessages(concurrencyMessages));
        hpanUpdatedEvents.addAll(buildValidPayloadsNotPaymentManagerChennel(0,dbElementsNumbers));

        long startTest = System.currentTimeMillis();
        hpanUpdatedEvents.forEach(e -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer,null, readUserId(e),e));
        kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer, List.of(new RecordHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");
        long timeAfterSendHpanUpdateMessages = System.currentTimeMillis();

        int newHPans = (updatedHpanNumbers - dbElementsNumbers) / 2;
        waitForDB(dbElementsNumbers+1+ newHPans +dbElementsNumbers);
        long endTestWithoutAsserts = System.currentTimeMillis();

        checkValidMessages();
        checkErrorsPublished(notValidMessages, maxWaitingMs, errorUseCases);
        checkConcurrencyMessages();
        checkHpanUpdatePublished(maxWaitingMs);
        checkUserInitiativeCounters(newHPans);

        System.out.printf("""
            ************************
            Time spent to send %d messages (from start): %d millis
            Test Completed in %d millis
            ************************
            """,
                hpanUpdatedEvents.size(),
                timeAfterSendHpanUpdateMessages-startTest,
                endTestWithoutAsserts-startTest
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        final Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = kafkaTestUtilitiesService.checkCommittedOffsets(topicHpanInitiativeLookupConsumer, groupIdHpanInitiativeLookupConsumer,hpanUpdatedEvents.size()+1); // +1 due to other applicationName useCase
        long timeCommitCheckEnd = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        ************************
                        """,
                timeCommitCheckEnd - timeCommitCheckStart,
                srcCommitOffsets
        );
    }

    private void checkValidMessages() {
        List<Mono<HpanInitiatives>> closeIntervals = IntStream.range(0, dbElementsNumbers /2).mapToObj(i -> hpanInitiativesRepository.findById("HPAN_%s".formatted(i))).toList();
        closeIntervals.forEach(hpanInitiativesMono -> {
            HpanInitiatives hpanInitiativeResult= hpanInitiativesMono.block();
            assert hpanInitiativeResult != null;
            if(Integer.parseInt(hpanInitiativeResult.getHpan().substring(5))%2 == 0) {
                Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                Assertions.assertEquals(3, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
            }
            else {
                Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                Assertions.assertEquals(2, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
            }
        });

        List<Mono<HpanInitiatives>> openIntervals = IntStream.range(dbElementsNumbers /2, dbElementsNumbers).mapToObj(i -> hpanInitiativesRepository.findById("HPAN_%s".formatted(i))).toList();
        openIntervals.forEach(hpanInitiativesMono -> {
            HpanInitiatives hpanInitiativeResult= hpanInitiativesMono.block();
            assert hpanInitiativeResult != null;
            if(Integer.parseInt(hpanInitiativeResult.getHpan().substring(5))%2 == 0) {
                Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                Assertions.assertEquals(3, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
            }else {
                Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                Assertions.assertEquals(2, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
            }
        });

        List<Mono<HpanInitiatives>> newHpans = IntStream.range(dbElementsNumbers, updatedHpanNumbers)
                .mapToObj(i -> hpanInitiativesRepository.findById("HPAN_%s".formatted(i))).toList();

        newHpans.forEach(hpanInitiativesMono -> {
            if (Boolean.TRUE.equals(hpanInitiativesMono.hasElement().block())) {
                HpanInitiatives hpanInitiativeResult = hpanInitiativesMono.block();
                assert hpanInitiativeResult != null;
                if (Integer.parseInt(hpanInitiativeResult.getHpan().substring(5))%2 == 0) {
                    Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                    Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
                }
            }
        });

        //region Check for with Payment Manager Channel
        List<Mono<HpanInitiatives>> closeIntervalsChannelPaymentManager = IntStream.range(0, dbElementsNumbers /2).mapToObj(i -> hpanInitiativesRepository.findById("HPAN_NOT_PAYMENT_MANAGER_CHANNEL_%s".formatted(i))).toList();
        closeIntervalsChannelPaymentManager.forEach(hpanInitiativesMono -> {
            HpanInitiatives hpanInitiativeResult= hpanInitiativesMono.block();
            assert hpanInitiativeResult != null;
            if(Integer.parseInt(hpanInitiativeResult.getHpan().substring(33))%2 == 0) {
                Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                Assertions.assertEquals(3, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
            }
            else {
                Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                Assertions.assertEquals(2, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
            }
        });

        List<Mono<HpanInitiatives>> openIntervalsChannelPaymentManager = IntStream.range(dbElementsNumbers /2, dbElementsNumbers).mapToObj(i -> hpanInitiativesRepository.findById("HPAN_NOT_PAYMENT_MANAGER_CHANNEL_%s".formatted(i))).toList();
        openIntervalsChannelPaymentManager.forEach(hpanInitiativesMono -> {
            HpanInitiatives hpanInitiativeResult= hpanInitiativesMono.block();
            assert hpanInitiativeResult != null;
            if(Integer.parseInt(hpanInitiativeResult.getHpan().substring(33))%2 == 0) {
                Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                Assertions.assertEquals(3, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
            }else {
                Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                Assertions.assertEquals(2, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
            }
        });
        //endregion
    }

    void initializeDB(int requestElements){
        int initiativesWithCloseIntervals = requestElements/2;
        IntStream.range(0, initiativesWithCloseIntervals).
                mapToObj(i -> HpanInitiativesFaker.mockInstanceWithCloseIntervals(i, INITIATIVE_ID_PF))
                .forEach(h -> hpanInitiativesRepository.save(h).subscribe(hSaved -> log.debug("saved hpan: {}", hSaved.getHpan())));
        IntStream.range(initiativesWithCloseIntervals, requestElements)
                .mapToObj(i ->HpanInitiativesFaker.mockInstance(i, INITIATIVE_ID_PF))
                .forEach(h -> hpanInitiativesRepository.save(h).subscribe(hSaved -> log.debug("saved hpan: {}", hSaved.getHpan())));
        initializeConcurrencyCase();
        initializeChannelManagementChannel(requestElements);
        waitForDB(requestElements+1+requestElements);

        initializeFamily();

    }

    private void initializeFamily() {
        List<OnboardingFamilies> ofList = IntStream.range(minBiasForNFInitiative+1, updatedHpanNumbers)
                .mapToObj(i -> OnboardingFamiliesFaker.mockInstanceBuilder(i)
                        .initiativeId(INITIATIVE_ID_NF)
                        .memberIds(Set.of("USERID_%d".formatted(i))).build())
                . toList();

        onboardingFamiliesRepository.saveAll(ofList).collectList().block();
    }

    private void initializeConcurrencyCase(){
        List<OnboardedInitiative> onboardedInitiatives = new ArrayList<>();
        LocalDateTime start = LocalDateTime.of(2022,9,5,0,0);
        LocalDateTime end = LocalDateTime.of(2022,9,10, 23, 59, 59,999999);
        ActiveTimeInterval activeTimeInterval = ActiveTimeInterval.builder().startInterval(start).endInterval(end).build();
        List<ActiveTimeInterval> activeTimeIntervalList = new ArrayList<>();
        activeTimeIntervalList.add(activeTimeInterval);

        OnboardedInitiative onboardedInitiative1 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_1").lastEndInterval(end).activeTimeIntervals(activeTimeIntervalList).build();
        onboardedInitiatives.add(onboardedInitiative1);
        OnboardedInitiative onboardedInitiative2 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_2").lastEndInterval(end).activeTimeIntervals(activeTimeIntervalList).build();
        onboardedInitiatives.add(onboardedInitiative2);

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID_CONCURRENCY")
                .hpan("HPAN_CONCURRENCY")
                .onboardedInitiatives(onboardedInitiatives).build();
        hpanInitiativesRepository.save(hpanInitiatives).subscribe(h -> log.info("saved hpan: {}", h.getHpan()));
    }

    private void initializeChannelManagementChannel(int requestElements) {
        int initiativesWithCloseIntervals = requestElements/2;
        IntStream.range(0, initiativesWithCloseIntervals).
                mapToObj(i -> {
                    HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(i);
                    hpanInitiatives.setHpan("HPAN_NOT_PAYMENT_MANAGER_CHANNEL_%d".formatted(i));
                    return hpanInitiatives;
                })
                .forEach(h -> hpanInitiativesRepository.save(h).subscribe(hSaved -> log.debug("saved hpan: {}", hSaved.getHpan())));
        IntStream.range(initiativesWithCloseIntervals, requestElements)
                .mapToObj(i -> {
                    HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(i);
                    hpanInitiatives.setHpan("HPAN_NOT_PAYMENT_MANAGER_CHANNEL_%d".formatted(i));
                    return hpanInitiatives;
                })
                .forEach(h -> hpanInitiativesRepository.save(h).subscribe(hSaved -> log.debug("saved hpan: {}", hSaved.getHpan())));
    }

    private void initializeRules(){
        InitiativeReward2BuildDTO initiativePF = InitiativeReward2BuildDTOFaker.mockInstance(1);
        initiativePF.setInitiativeId(INITIATIVE_ID_PF);
        initiativePF.getGeneral().setBeneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF);

        InitiativeReward2BuildDTO initiativeNF = InitiativeReward2BuildDTOFaker.mockInstance(1);
        initiativeNF.setInitiativeId(INITIATIVE_ID_NF);
        initiativeNF.getGeneral().setBeneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF);

        Stream.of(initiativePF, initiativeNF)
                        .map(TestUtils::jsonSerializer)
                                .forEach(m -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, m) );

        RewardRuleConsumerConfigTestDeprecated.waitForKieContainerBuild(2, rewardContextHolderService);
    }
    private void waitForDB(int N) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        TestUtils.waitFor(()->(countSaved[0]=hpanInitiativesRepository.count().block()) >= N, ()->"Expected %d saved initiatives, read %d".formatted(N, countSaved[0]), 60, 1000);
    }

    private List<String> buildValidPayloads(int start, int end) {
        return IntStream.range(start, end)
                .mapToObj(i -> HpanInitiativeBulkDTOFaker.mockInstanceBuilder(i)
                        .operationDate(LocalDateTime.now().plusDays(10L))
                        .operationType(i%2 == 0 ? HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT : HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                        .channel(i%2 == 0 ? HpanInitiativeConstants.CHANEL_PAYMENT_MANAGER : HpanInitiativeConstants.CHANNEL_IDPAY_PAYMENT)
                        .initiativeId(i > minBiasForNFInitiative ? INITIATIVE_ID_NF : INITIATIVE_ID_PF)
                        .build())
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private Collection<String> buildValidPayloadsNotPaymentManagerChennel(int start, int end) {
        return IntStream.range(start, end)
                .mapToObj(i -> {
                    PaymentMethodInfoDTO info = PaymentMethodInfoDTO.builder()
                            .hpan("HPAN_NOT_PAYMENT_MANAGER_CHANNEL_%d".formatted(i))
                            .maskedPan("MASKEDPAN_NOT_PAYMENT_MANAGER_CHANNEL_%d".formatted(i))
                            .brandLogo("BRANDLOGO_NOT_PAYMENT_MANAGER_CHANNEL_%d".formatted(i))
                            .build();
                    return HpanInitiativeBulkDTOFaker.mockInstanceBuilder(i)
                            .operationDate(LocalDateTime.now())
                            .operationType(i % 2 == 0 ? HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT : HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                            .channel("ANOTHER_CHANNEL")
                            .infoList(List.of(info))
                            .build();
                })
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private List<String> buildConcurrencyMessages(int concurrencyMessagesNumber){
        return IntStream.range(1, concurrencyMessagesNumber+1).mapToObj(i -> {
                    PaymentMethodInfoDTO infoHpanConcurrency = PaymentMethodInfoDTO.builder()
                            .hpan("HPAN_CONCURRENCY")
                            .maskedPan("MASKEDPAN_CONCURRENCY")
                            .brandLogo("BRANDLOGO_CONCURRENCY").build();
                    return HpanInitiativeBulkDTO.builder()
                            .userId("USERID_CONCURRENCY")
                            .initiativeId("INITIATIVEID_%d".formatted(i))
                            .infoList(List.of(infoHpanConcurrency))
                            .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                            .operationDate(LocalDateTime.of(2022, 9, 15, 10, 45, 30))
                            .channel(HpanInitiativeConstants.CHANEL_PAYMENT_MANAGER)
                            .build();
                })
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    //region not valid useCases

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    // useCase0
    {
        String useCaseJsonNotHpan = "{\"initiativeId\":\"id_0\",\"userId\":\"userid_0\", \"operationType\":\"ADD_INSTRUMENT\",\"operationDate\":\"2022-08-27T10:58:30.053881354\"}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotHpan,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[HPAN_INITIATIVE_OP] An error occurred evaluating hpan update", useCaseJsonNotHpan, "userid_0")
        ));

        String useCaseJsonNotDate = "{\"initiativeId\":\"id_1\",\"userId\":\"userid_1\", \"operationType\":\"ADD_INSTRUMENT\",\"hpan\":\"hpan\"}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotDate,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[HPAN_INITIATIVE_OP] An error occurred evaluating hpan update", useCaseJsonNotDate, "userid_1")
        ));

        String jsonNotValid = "{\"initiativeId\":\"id_2\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[HPAN_INITIATIVE_OP] Unexpected JSON", jsonNotValid, "")
        ));

        final String failingHpanUpdateOutcomePublishingUserId = "FAILING_HPAN_UPDATE_OUTCOME_PUBLISHING";
        PaymentMethodInfoDTO infoFailingHpanUpdatePublishing = PaymentMethodInfoDTO.builder()
                .hpan("HPAN_%s".formatted(failingHpanUpdateOutcomePublishingUserId))
                .maskedPan("MASKEDPAN_%s".formatted(failingHpanUpdateOutcomePublishingUserId))
                .brandLogo("BRANDLOGO_%s".formatted(failingHpanUpdateOutcomePublishingUserId))
                .build();
        HpanInitiativeBulkDTO hpanBulkErrorPublishedNotPaymentManagerChannel = HpanInitiativeBulkDTO.builder()
                .initiativeId("id_%d".formatted(errorUseCases.size()))
                .userId(failingHpanUpdateOutcomePublishingUserId)
                .infoList(List.of(infoFailingHpanUpdatePublishing))
                .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                .channel("CHANNEL_FAILING_PUBLISHED")
                .build();
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(false).when(hpanUpdateNotifierServiceSpy).notify(Mockito.argThat(i -> failingHpanUpdateOutcomePublishingUserId.equals(i.getUserId())));
                    return TestUtils.jsonSerializer(hpanBulkErrorPublishedNotPaymentManagerChannel);
                },
                errorMessage -> {
                    HpanUpdateOutcomeDTO expectedFailingHpanUpdateOutcomePublishing = HpanUpdateOutcomeDTO.builder()
                            .initiativeId(hpanBulkErrorPublishedNotPaymentManagerChannel.getInitiativeId())
                            .userId(hpanBulkErrorPublishedNotPaymentManagerChannel.getUserId())
                            .hpanList(new ArrayList<>())
                            .rejectedHpanList(List.of(infoFailingHpanUpdatePublishing.getHpan()))
                            .operationType(hpanBulkErrorPublishedNotPaymentManagerChannel.getOperationType())
                            .timestamp(LocalDateTime.now())
                            .build();
                    checkErrorMessageHeaders(topicHpanUpdateOutcome,null, errorMessage,"[HPAN_UPDATE_OUTCOME] An error occurred while publishing the hpan update outcome",TestUtils.jsonSerializer(expectedFailingHpanUpdateOutcomePublishing),null,false,false);
                }
        ));

        // useCase1
        final String failingExceptionHpanUpdateOutcomePublishingUserId = "FAILING_HPAN_UPDATE_OUTCOME_PUBLISHING_DUE_EXCEPTION";
        PaymentMethodInfoDTO infoFailingExceptionHpanUpdatePublishing = PaymentMethodInfoDTO.builder()
                .hpan("HPAN_%s".formatted(failingExceptionHpanUpdateOutcomePublishingUserId))
                .maskedPan("MASKEDPAN_%s".formatted(failingExceptionHpanUpdateOutcomePublishingUserId))
                .brandLogo("BRANDLOGO_%s".formatted(failingExceptionHpanUpdateOutcomePublishingUserId))
                .build();
        HpanInitiativeBulkDTO hpanBulkErrorPublishedExceptionNotPaymentManagerChannel = HpanInitiativeBulkDTO.builder()
                .initiativeId("id_%d".formatted(errorUseCases.size()))
                .userId(failingExceptionHpanUpdateOutcomePublishingUserId)
                .infoList(List.of(infoFailingExceptionHpanUpdatePublishing))
                .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                .channel("CHANNEL_FAILING_EXCEPTION_PUBLISHED")
                .build();
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(false).when(hpanUpdateNotifierServiceSpy).notify(Mockito.argThat(i -> failingExceptionHpanUpdateOutcomePublishingUserId.equals(i.getUserId())));
                    return TestUtils.jsonSerializer(hpanBulkErrorPublishedExceptionNotPaymentManagerChannel);
                },
                errorMessage -> {
                    HpanUpdateOutcomeDTO expectedFailingHpanUpdateOutcomePublishing = HpanUpdateOutcomeDTO.builder()
                            .initiativeId(hpanBulkErrorPublishedExceptionNotPaymentManagerChannel.getInitiativeId())
                            .userId(hpanBulkErrorPublishedExceptionNotPaymentManagerChannel.getUserId())
                            .hpanList(new ArrayList<>())
                            .rejectedHpanList(List.of(infoFailingExceptionHpanUpdatePublishing.getHpan()))
                            .operationType(hpanBulkErrorPublishedExceptionNotPaymentManagerChannel.getOperationType())
                            .timestamp(LocalDateTime.now())
                            .build();
                    checkErrorMessageHeaders(topicHpanUpdateOutcome,null, errorMessage,"[HPAN_UPDATE_OUTCOME] An error occurred while publishing the hpan update outcome",TestUtils.jsonSerializer(expectedFailingHpanUpdateOutcomePublishing),null,false,false);
                }
        ));
    }
    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(topicHpanInitiativeLookupConsumer, groupIdHpanInitiativeLookupConsumer, errorMessage, errorDescription, expectedPayload, expectedKey);
    }

    //endregion
    private void checkConcurrencyMessages(){
        HpanInitiatives hpanConcurrecy = hpanInitiativesRepository.findById("HPAN_CONCURRENCY").block();
        Assertions.assertNotNull(hpanConcurrecy);

        List<OnboardedInitiative> onboardedInitiatives = hpanConcurrecy.getOnboardedInitiatives();
        Assertions.assertEquals(2, onboardedInitiatives.size());

        onboardedInitiatives.forEach(onboardedInitiative ->
                Assertions.assertEquals(2, onboardedInitiative.getActiveTimeIntervals().size()));
    }

    private void checkHpanUpdatePublished(long maxWaitingMs) {
        List<ConsumerRecord<String, String>> consumerRecords = kafkaTestUtilitiesService.consumeMessages(topicHpanUpdateOutcome, dbElementsNumbers, maxWaitingMs);
        Assertions.assertEquals(dbElementsNumbers,consumerRecords.size());
        consumerRecords.forEach(cr -> {
            try {
                HpanUpdateOutcomeDTO hpanUpdateOutcomeDTO = objectMapper.readValue(cr.value(), HpanUpdateOutcomeDTO.class);
                Assertions.assertNotNull(hpanUpdateOutcomeDTO);
                TestUtils.checkNotNullFields(hpanUpdateOutcomeDTO);

                int bias = Integer.parseInt(hpanUpdateOutcomeDTO.getUserId().substring(7));

                Assertions.assertNotNull(hpanUpdateOutcomeDTO.getHpanList());
                if(bias<dbElementsNumbers/2){
                    if(bias%2==0){
                        Assertions.assertEquals(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT, hpanUpdateOutcomeDTO.getOperationType());
                        Assertions.assertTrue(hpanUpdateOutcomeDTO.getHpanList().contains("HPAN_NOT_PAYMENT_MANAGER_CHANNEL_%d".formatted(bias)));
                        Assertions.assertEquals(0, hpanUpdateOutcomeDTO.getRejectedHpanList().size());
                    }else {
                        Assertions.assertEquals(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT, hpanUpdateOutcomeDTO.getOperationType());
                        Assertions.assertEquals(0, hpanUpdateOutcomeDTO.getHpanList().size());
                        Assertions.assertTrue(hpanUpdateOutcomeDTO.getRejectedHpanList().contains("HPAN_NOT_PAYMENT_MANAGER_CHANNEL_%d".formatted(bias)));
                    }
                }else {
                    if(bias%2==0){
                        Assertions.assertEquals(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT, hpanUpdateOutcomeDTO.getOperationType());
                    } else {
                        Assertions.assertEquals(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT, hpanUpdateOutcomeDTO.getOperationType());
                    }
                    Assertions.assertTrue(hpanUpdateOutcomeDTO.getHpanList().contains("HPAN_NOT_PAYMENT_MANAGER_CHANNEL_%d".formatted(bias)));
                    Assertions.assertEquals(0, hpanUpdateOutcomeDTO.getRejectedHpanList().size());
                }
            } catch (JsonProcessingException e) {
                Assertions.fail();
            }
        });
    }

    private final Pattern userIdPatternMatch = Pattern.compile("\"userId\":\"([^\"]*)\"");

    private String readUserId(String payload) {
        final Matcher matcher = userIdPatternMatch.matcher(payload);
        return matcher.find() ? matcher.group(1) : "";
    }

    private void checkUserInitiativeCounters(int newHPans) {
        int[] i = new int[]{dbElementsNumbers};
        Assertions.assertEquals(newHPans, userInitiativeCountersRepository.count().block());
        Objects.requireNonNull(userInitiativeCountersRepository.findAll()
                                .sort(Comparator.comparing(UserInitiativeCounters::getEntityId,
                                        Comparator.comparing(u -> u.substring(7))))
                .collectList()
                .block()
                )
                .forEach(c->{
                    int bias = Integer.parseInt(c.getEntityId().substring(7));
                    Assertions.assertEquals(i[0], bias);
                    i[0]+=2;

                    UserInitiativeCounters expectedCounter;
                    if(bias > minBiasForNFInitiative){
                        expectedCounter = new UserInitiativeCounters("FAM.ID_" +bias, InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, INITIATIVE_ID_NF);
                    } else {
                        expectedCounter = new UserInitiativeCounters("USERID_" + bias, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF ,INITIATIVE_ID_PF);
                    }
                    Assertions.assertTrue(c.getUpdateDate().isBefore(expectedCounter.getUpdateDate()));
                    Assertions.assertTrue(c.getUpdateDate().isAfter(expectedCounter.getUpdateDate().truncatedTo(ChronoUnit.HOURS)));
                    expectedCounter.setUpdateDate(c.getUpdateDate());
                    Assertions.assertEquals(expectedCounter, c);
                });
    }
}