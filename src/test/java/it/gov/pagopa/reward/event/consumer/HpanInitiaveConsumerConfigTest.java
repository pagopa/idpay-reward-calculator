package it.gov.pagopa.reward.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateOutcomeDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.service.lookup.HpanUpdateNotifierService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.lookup.HpanInitiativeMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.lookup.HpanInitiativesServiceImpl=DEBUG",
        "logging.level.it.gov.pagopa.reward.service.lookup.ops.AddHpanServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.lookup.ops.DeleteHpanServiceImpl=OFF",
})
class HpanInitiaveConsumerConfigTest extends BaseIntegrationTest {
    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;

    @SpyBean
    private HpanUpdateNotifierService hpanUpdateNotifierServiceSpy;

    @AfterEach
    void cleanData(){
        hpanInitiativesRepository.deleteAll().block();
    }
    @Test
    void hpanInitiativeConsumer() {
        int dbElementsNumbers = 200;
        int updatedHpanNumbers = 1000;
        int notValidMessages = errorUseCases.size();
        int concurrencyMessages = 2;
        long maxWaitingMs = 30000;

        initializeDB(dbElementsNumbers);

        List<String> hpanUpdatedEvents = new ArrayList<>(buildValidPayloads(0,dbElementsNumbers));
        hpanUpdatedEvents.addAll(IntStream.range(0, notValidMessages).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        hpanUpdatedEvents.addAll(buildValidPayloads(dbElementsNumbers,updatedHpanNumbers));
        hpanUpdatedEvents.addAll(buildConcurrencyMessages(concurrencyMessages));
        hpanUpdatedEvents.addAll(buildValidPayloadsNotPaymentManagerChennel(0,dbElementsNumbers));

        long startTest = System.currentTimeMillis();
        hpanUpdatedEvents.forEach(e -> publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer,null, readUserId(e),e));
        publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer, List.of(new RecordHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");
        long timeAfterSendHpanUpdateMessages = System.currentTimeMillis();

        waitForDB(dbElementsNumbers+1+((updatedHpanNumbers-dbElementsNumbers)/2)+dbElementsNumbers);
        long endTestWithoutAsserts = System.currentTimeMillis();

        checkValidMessages(dbElementsNumbers, updatedHpanNumbers);
        checkErrorsPublished(notValidMessages, maxWaitingMs, errorUseCases);
        checkConcurrencyMessages();
        checkHpanUpdatePublished(dbElementsNumbers,maxWaitingMs);

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
        final Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicHpanInitiativeLookupConsumer, groupIdHpanInitiativeLookupConsumer,hpanUpdatedEvents.size()+1); // +1 due to other applicationName useCase
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

    private void checkValidMessages(int dbElementsNumbers, int updatedHpanNumbers) {
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

        //region Check for whit Payment Manager Channel
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
                mapToObj(HpanInitiativesFaker::mockInstanceWithCloseIntervals)
                .forEach(h -> hpanInitiativesRepository.save(h).subscribe(hSaved -> log.debug("saved hpan: {}", hSaved.getHpan())));
        IntStream.range(initiativesWithCloseIntervals, requestElements)
                .mapToObj(HpanInitiativesFaker::mockInstance)
                .forEach(h -> hpanInitiativesRepository.save(h).subscribe(hSaved -> log.debug("saved hpan: {}", hSaved.getHpan())));
        initializeConcurrencyCase();
        initializeChannelManagementChannel(requestElements);
        waitForDB(requestElements+1+requestElements);

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

    private void waitForDB(int N) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=hpanInitiativesRepository.count().block()) >= N, ()->"Expected %d saved initiatives, read %d".formatted(N, countSaved[0]), 60, 1000);
    }

    private List<String> buildValidPayloads(int start, int end) {
        return IntStream.range(start, end)
                .mapToObj(i -> HpanInitiativeBulkDTOFaker.mockInstanceBuilder(i)
                        .operationDate(LocalDateTime.now().plusDays(10L))
                        .operationType(i%2 == 0 ? HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT : HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                        .channel(i%2 == 0 ? HpanInitiativeConstants.CHANEL_PAYMENT_MANAGER : HpanInitiativeConstants.CHANNEL_IDPAY_PAYMENT)
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

    private void checkHpanUpdatePublished(int updatedHpanNumbers, long maxWaitingMs) {
        List<ConsumerRecord<String, String>> consumerRecords = consumeMessages(topicHpanUpdateOutcome, updatedHpanNumbers, maxWaitingMs);
        Assertions.assertEquals(updatedHpanNumbers,consumerRecords.size());
        consumerRecords.forEach(cr -> {
            try {
                HpanUpdateOutcomeDTO hpanUpdateOutcomeDTO = objectMapper.readValue(cr.value(), HpanUpdateOutcomeDTO.class);
                Assertions.assertNotNull(hpanUpdateOutcomeDTO);
                TestUtils.checkNotNullFields(hpanUpdateOutcomeDTO);

                int bias = Integer.parseInt(hpanUpdateOutcomeDTO.getUserId().substring(7));

                Assertions.assertNotNull(hpanUpdateOutcomeDTO.getHpanList());
                if(bias<updatedHpanNumbers/2){
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
}