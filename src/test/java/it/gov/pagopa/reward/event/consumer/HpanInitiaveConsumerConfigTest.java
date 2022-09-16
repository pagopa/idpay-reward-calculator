package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.lookup.HpanInitiativeMediatorServiceImpl=INFO",
        "logging.level.it.gov.pagopa.reward.service.lookup.HpanInitiativesServiceImpl=DEBUG",
        "logging.level.it.gov.pagopa.reward.service.lookup.ops.AddHpanServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.lookup.ops.DeleteHpanServiceImpl=OFF",
})
class HpanInitiaveConsumerConfigTest extends BaseIntegrationTest {
    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Test
    void hpanInitiativeConsumer() {
        int dbElementsNumbers = 20;
        int updatedHpanNumbers = 100;
        int notValidMessages = errorUseCases.size();
        int concurrencyMessages = 2;
        long maxWaitingMs = 30000;

        initializeDB(dbElementsNumbers);
        long startTest = System.currentTimeMillis();

        List<String> hpanUpdatedEvents = new ArrayList<>(buildValidPayloads(0,dbElementsNumbers));
        hpanUpdatedEvents.addAll(IntStream.range(0, notValidMessages).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        hpanUpdatedEvents.addAll(buildValidPayloads(dbElementsNumbers,updatedHpanNumbers));
        hpanUpdatedEvents.addAll(buildConcurrencyMessages(concurrencyMessages));

        hpanUpdatedEvents.forEach(e -> publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer,null, readUserId(e),e));
        long timeAfterSendHpanUpdateMessages = System.currentTimeMillis();

        waitForDB(dbElementsNumbers+1+((updatedHpanNumbers-dbElementsNumbers)/2));
        long endTestWithoutAsserts = System.currentTimeMillis();

        checkValidMessages(dbElementsNumbers, updatedHpanNumbers);
        checkErrorsPublished(notValidMessages, maxWaitingMs, errorUseCases);
        checkConcurrencyMessages();

        System.out.printf("""
            ************************
            Time spent to send %d messages (from start): %d millis
            Test Completed in %d millis
            ************************
            """,
                updatedHpanNumbers+notValidMessages+concurrencyMessages, timeAfterSendHpanUpdateMessages-startTest,
                endTestWithoutAsserts-startTest
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
        waitForDB(requestElements+1);

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
    private void waitForDB(int N) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=hpanInitiativesRepository.count().block()) >= N, ()->"Expected %d saved initiatives, read %d".formatted(N, countSaved[0]), 60, 1000);
    }

    private List<String> buildValidPayloads(int start, int end) {
        return IntStream.range(start, end)
                .mapToObj(i -> HpanInitiativeBulkDTOFaker.mockInstanceBuilder(i)
                        .operationDate(LocalDateTime.now().plusDays(10L))
                        .operationType(i%2 == 0 ? HpanInitiativeConstants.ADD_INSTRUMENT : HpanInitiativeConstants.DELETE_INSTRUMENT).build())
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private List<String> buildConcurrencyMessages(int concurrencyMessagesNumber){
        return IntStream.range(1, concurrencyMessagesNumber+1).mapToObj(i -> HpanInitiativeBulkDTO.builder()
                .userId("USERID_CONCURRENCY")
                .initiativeId("INITIATIVEID_%d".formatted(i))
                .hpanList(List.of("HPAN_CONCURRENCY"))
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT)
                .operationDate(LocalDateTime.of(2022, 9, 15, 10, 45, 30)).build())
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    //region not valid useCases
    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotHpan = "{\"initiativeId\":\"id_0\",\"userId\":\"userid_0\", \"operationType\":\"ADD_INSTRUMENT\",\"operationDate\":\"2022-08-27T10:58:30.053881354\"}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotHpan,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred evaluating hpan update", useCaseJsonNotHpan)
        ));

        String useCaseJsonNotDate = "{\"initiativeId\":\"id_1\",\"userId\":\"userid_1\", \"operationType\":\"ADD_INSTRUMENT\",\"hpan\":\"hpan\"}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotDate,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred evaluating hpan update", useCaseJsonNotDate)
        ));

        String jsonNotValid = "{\"initiativeId\":\"id_2\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "Unexpected JSON", jsonNotValid)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicHpanInitiativeLookupConsumer, errorMessage, errorDescription, expectedPayload);
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

    private final Pattern userIdPatternMatch = Pattern.compile("\"userId\":\"([^\"]*)\"");

    private String readUserId(String payload) {
        final Matcher matcher = userIdPatternMatch.matcher(payload);
        return matcher.find() ? matcher.group(1) : "";
    }
}