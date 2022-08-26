package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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

        initializeDB(dbElementsNumbers);
        long startTest = System.currentTimeMillis();
        //region prepared events
        List<HpanInitiativeDTO> hpanInitiativeDTOValids = IntStream.range(0, dbElementsNumbers)
                .mapToObj(i -> HpanInitiativeDTOFaker.mockInstanceBuilder(i)
                        .operationDate(LocalDateTime.now().plusDays(10L))
                        .operationType(i%2 == 0 ? HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name() : HpanInitiativeDTO.OperationType.DELETE_INSTRUMENT.name()).build())
                .toList();
        List<HpanInitiativeDTO> hpanUpdatedEvents = new ArrayList<>(hpanInitiativeDTOValids);

        List<HpanInitiativeDTO> hpanInitiativeDTOAnothers = IntStream.range(dbElementsNumbers, updatedHpanNumbers)
                .mapToObj(i -> HpanInitiativeDTOFaker.mockInstanceBuilder(i)
                        .operationDate(LocalDateTime.now().plusDays(10L))
                        .operationType(i%2 == 0 ? HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name() : HpanInitiativeDTO.OperationType.DELETE_INSTRUMENT.name())
                        .build()).toList();
        hpanUpdatedEvents.addAll(hpanInitiativeDTOAnothers);
        //endregion

        hpanUpdatedEvents.forEach(e -> publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer,null, null,e));
        long timeAfterSendHpanUpdateMessages = System.currentTimeMillis();

        waitForDB(dbElementsNumbers+((updatedHpanNumbers-dbElementsNumbers)/2));

        long endTestWithoutAsserts = System.currentTimeMillis();

        //Prendere a DB gli elementi aggiornati e controllare che
        List<Mono<HpanInitiatives>> closeIntervals = IntStream.range(0, dbElementsNumbers/2).mapToObj(i -> hpanInitiativesRepository.findById("HPAN_%s".formatted(i))).toList();
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

        List<Mono<HpanInitiatives>> openIntervals = IntStream.range(dbElementsNumbers/2,dbElementsNumbers).mapToObj(i -> hpanInitiativesRepository.findById("HPAN_%s".formatted(i))).toList();
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
        System.out.printf("""
            ************************
            Time spent to send %d messages (from start): %d millis
            Test Completed in %d millis
            ************************
            """,
                updatedHpanNumbers, timeAfterSendHpanUpdateMessages-startTest,
                endTestWithoutAsserts-startTest
        );
    }

    void initializeDB(int requestElements){
        int initiativesWithCloseIntervals = requestElements/2;
        IntStream.range(0, initiativesWithCloseIntervals).
                mapToObj(HpanInitiativesFaker::mockInstanceWithCloseIntervals)
                .forEach(h -> {hpanInitiativesRepository.save(h).subscribe(hSaved -> log.info("saved hpan: {}", hSaved.getHpan()));});
        IntStream.range(initiativesWithCloseIntervals, requestElements)
                .mapToObj(HpanInitiativesFaker::mockInstance)
                .forEach(h -> {hpanInitiativesRepository.save(h).subscribe(hSaved -> log.info("saved hpan: {}", hSaved.getHpan()));});
        waitForDB(requestElements);
    }

    private long waitForDB(int N) {
        long[] countSaved={0};
        waitFor(()->(countSaved[0]=hpanInitiativesRepository.count().block()) >= N, ()->"Expected %d saved initiatives, read %d".formatted(N, countSaved[0]), 60, 1000);
        return countSaved[0];
    }
}