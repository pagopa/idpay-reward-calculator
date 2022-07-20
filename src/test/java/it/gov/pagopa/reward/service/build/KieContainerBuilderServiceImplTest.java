package it.gov.pagopa.reward.service.build;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;

class KieContainerBuilderServiceImplTest {

    @BeforeAll
    public static void configDroolsLogs() {
        ((Logger) LoggerFactory.getLogger("org.kie.api.internal.utils")).setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("org.drools")).setLevel(Level.INFO);
    }

    @Test
    void buildAllNotFindRules() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        KieContainerBuilderService kieContainerBuilderService = new KieContainerBuilderServiceImpl(droolsRuleRepository);

        Mockito.when(droolsRuleRepository.findAll()).thenReturn(Flux.empty());
        // When
        KieContainer result = kieContainerBuilderService.buildAll().block();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepository).findAll();
    }


    //TODO
    @Test
    void buildAllFindRules() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        KieContainerBuilderService kieContainerBuilderService = new KieContainerBuilderServiceImpl(droolsRuleRepository);

        DroolsRule dr = new DroolsRule();
        dr.setId("InitiativeID");
        dr.setName("name");
        String ruleCondition = "eval(true)";
        String ruleConsequence = "System.out.println(\"EXECUTED\");";
        //TODO implement field
        //dr.setRule(ExtraFilter2DroolsTransformerImplTest.applyRuleTemplate(dr.getId(), dr.getName(), ruleCondition, ruleConsequence));

        Flux<DroolsRule> rulesFlux = Flux.just(dr);
        Mockito.when(droolsRuleRepository.findAll()).thenReturn(rulesFlux);
        // When
        KieContainer result = kieContainerBuilderService.buildAll().block();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepository).findAll();
    }
}