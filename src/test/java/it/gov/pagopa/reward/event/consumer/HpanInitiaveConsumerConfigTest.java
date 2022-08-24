package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl=DEBUG",
})
class HpanInitiaveConsumerConfigTest extends BaseIntegrationTest {

    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;

    @Test
    void hpanInitiativeConsumer() throws InterruptedException {
        int dbElementsNumbers = 2;
        int updatedHpanNumbers = 10;
        //Popolare DB con elementi
        initializeDB(dbElementsNumbers);

        //send into kafka del aggiornamento validi e non validi
        //

        List<HpanInitiativeDTO> hpanInitiativeDTOValids = IntStream.range(0, dbElementsNumbers).mapToObj(HpanInitiativeDTOFaker::mockInstance).toList();
        List<HpanInitiativeDTO> hpanUpdatedEvents = new ArrayList<>(hpanInitiativeDTOValids);

        List<HpanInitiativeDTO> hpanInitiativeDTOAnothers = IntStream.range(dbElementsNumbers, updatedHpanNumbers - dbElementsNumbers).mapToObj(HpanInitiativeDTOFaker::mockInstance).toList();
        hpanUpdatedEvents.addAll(hpanInitiativeDTOAnothers);

        hpanUpdatedEvents.forEach(e -> publishIntoEmbeddedKafka(topicHpanInitiativeLookupConsumer,null, null,e));

        //Attendere tempo di processamento
        Thread.sleep(1000L); //TODO remove me

        //Prendere a DB gli elementi aggiornati e controllare che

    }

    void initializeDB(int requestElements){
        //TODO add another cases
        List<HpanInitiatives> hpanInitiatives = IntStream.range(0, requestElements).mapToObj(i -> HpanInitiativesFaker.mockInstance(requestElements)).toList();
        hpanInitiativesRepository.saveAll(hpanInitiatives);
    }
}