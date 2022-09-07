package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
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
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl=DEBUG",
})
class HpanInitiaveConsumerConfigTest extends BaseIntegrationTest {
    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Test
    void hpanInitiativeConsumer() {
        int dbElementsNumbers = 200;
        int updatedHpanNumbers = 1000;
        int notValidRules = errorUseCases.size();
        long maxWaitingMs = 30000;

        initializeDB(dbElementsNumbers);
        long startTest = System.currentTimeMillis();

        List<String> hpanUpdatedEvents = new ArrayList<>(buildValidPayloads(0,dbElementsNumbers));
        hpanUpdatedEvents.addAll(IntStream.range(0, notValidRules).mapToObj(i -> errorUseCases.get(i).getFirst()).toList());
        hpanUpdatedEvents.addAll(buildValidPayloads(dbElementsNumbers,updatedHpanNumbers));

        hpanUpdatedEvents.forEach(e -> publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer,null, null,e));
        long timeAfterSendHpanUpdateMessages = System.currentTimeMillis();

        waitForDB(dbElementsNumbers+((updatedHpanNumbers-dbElementsNumbers)/2));

        // consume error messages
        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> errorsConsumed = consumeMessages(topicErrors, notValidRules, maxWaitingMs);
        long endTestWithoutAsserts = System.currentTimeMillis();

        // TODO fix
//        checkValidMessages(dbElementsNumbers, updatedHpanNumbers);
        checkErrorsPublished(notValidRules, errorsConsumed);

        System.out.printf("""
            ************************
            Time spent to send %d messages (from start): %d millis
            Time to consume %d messages from errorTopic: %d millis
            Test Completed in %d millis
            ************************
            """,
                updatedHpanNumbers+notValidRules, timeAfterSendHpanUpdateMessages-startTest,
                notValidRules, endTestWithoutAsserts-timeConsumerResponse,
                endTestWithoutAsserts-startTest
        );
    }

    private void checkValidMessages(int dbElementsNumbers, int updatedHpanNumbers) {
        List<Mono<HpanInitiatives>> closeIntervals = IntStream.range(0, dbElementsNumbers /2).mapToObj(i -> hpanInitiativesRepository.findById("HPAN_%s".formatted(i))).toList();
        closeIntervals.forEach(hpanInitiativesMono -> {
            HpanInitiatives hpanInitiativeResult= hpanInitiativesMono.block();
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
            Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
            Assertions.assertEquals(2, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
        });

        List<Mono<HpanInitiatives>> newHpans = IntStream.range(dbElementsNumbers, updatedHpanNumbers)
                .mapToObj(i -> hpanInitiativesRepository.findById("HPAN_%s".formatted(i))).toList();

        newHpans.forEach(hpanInitiativesMono -> {
            if (Boolean.TRUE.equals(hpanInitiativesMono.hasElement().block())) {
                HpanInitiatives hpanInitiativeResult = hpanInitiativesMono.block();
                if (Integer.parseInt(hpanInitiativeResult.getHpan().substring(5))%2 == 0) {
                    Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().size());
                    Assertions.assertEquals(1, hpanInitiativeResult.getOnboardedInitiatives().get(0).getActiveTimeIntervals().size());
                }
            }
        });
    }

    private void checkErrorsPublished(int notValidRules, List<ConsumerRecord<String, String>> payloadConsumed) {
        Assertions.assertEquals(notValidRules, payloadConsumed.size());
        for (ConsumerRecord<String, String> p : payloadConsumed) {
            Assertions.assertEquals(new String(p.headers().lastHeader("srcTopic").value()),topicHpanInitiativeLookupConsumer);
            Assertions.assertTrue(errorUseCases.contains(
                    Pair.of(p.value(),new String(p.headers().lastHeader("description").value()))
            ));
        }
    }

    void initializeDB(int requestElements){
        int initiativesWithCloseIntervals = requestElements/2;
        IntStream.range(0, initiativesWithCloseIntervals).
                mapToObj(HpanInitiativesFaker::mockInstanceWithCloseIntervals)
                .forEach(h -> hpanInitiativesRepository.save(h).subscribe(hSaved -> log.info("saved hpan: {}", hSaved.getHpan())));
        IntStream.range(initiativesWithCloseIntervals, requestElements)
                .mapToObj(HpanInitiativesFaker::mockInstance)
                .forEach(h -> hpanInitiativesRepository.save(h).subscribe(hSaved -> log.info("saved hpan: {}", hSaved.getHpan())));
        waitForDB(requestElements);
    }

    private long waitForDB(int N) {
        long[] countSaved={0};
        waitFor(()->(countSaved[0]=hpanInitiativesRepository.count().block()) >= N, ()->"Expected %d saved initiatives, read %d".formatted(N, countSaved[0]), 60, 1000);
        return countSaved[0];
    }

    private List<String> buildValidPayloads(int start, int end) {
        return IntStream.range(start, end)
                .mapToObj(i -> HpanInitiativeDTOFaker.mockInstanceBuilder(i)
                        .operationDate(LocalDateTime.now().plusDays(10L))
                        .operationType(i%2 == 0 ? HpanInitiativeConstants.ADD_INSTRUMENT : HpanInitiativeConstants.DELETE_INSTRUMENT).build())
                .map(TestUtils::jsonSerializer)
                .toList();
    }


    private final List<Pair<String,String>> errorUseCases = new ArrayList<>();
    {
        String evaluateErrorDescription = "An error occurred evaluating hpan update";
        String unexpectedJsonErrorDescription = "Unexpected JSON";

        String useCaseJsonNotHpan =  TestUtils.jsonSerializer(HpanInitiativeDTO.builder()
                .userId("userId").initiativeId("initiativeId").hpan("hpan").operationType(HpanInitiativeConstants.ADD_INSTRUMENT).build());//"{\"userId\":\"userid_0\",\"initiativeId\":\"id_0\", \"operationType\":\"ADD_INSTRUMENT\",\"hpan\":\"hpan\"}";
        errorUseCases.add(Pair.of(useCaseJsonNotHpan, evaluateErrorDescription));

        String useCaseJsonNotDate = TestUtils.jsonSerializer(HpanInitiativeDTO.builder()
                .userId("userId").initiativeId("initiativeId").hpan("hpan").operationType(HpanInitiativeConstants.ADD_INSTRUMENT).build());//"{\"userId\":\"userid_0\",\"initiativeId\":\"id_0\", \"operationType\":\"ADD_INSTRUMENT\",\"hpan\":\"hpan\"}";
        errorUseCases.add(Pair.of(useCaseJsonNotDate, evaluateErrorDescription));

        String jsonNotValid = "{\"initiativeId\":\"id_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(jsonNotValid, unexpectedJsonErrorDescription));
    }

}