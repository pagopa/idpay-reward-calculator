package it.gov.pagopa.reward.service.build;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

@Slf4j
public class KieContainerBuilderServiceImplTest {

    @BeforeAll
    public static void configDroolsLogs() {
        ((Logger) LoggerFactory.getLogger("org.kie.api.internal.utils")).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org.drools")).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.DEBUG);
    }

    @Test
    void buildAllNotFindRules() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        KieContainerBuilderService kieContainerBuilderService = new KieContainerBuilderServiceImpl(droolsRuleRepository);

        Mockito.when(droolsRuleRepository.findAll()).thenReturn(Flux.empty());
        // When
        KieBase result = kieContainerBuilderService.buildAll().block();

        // Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepository).findAll();
    }


    @Test
    void buildAllFindRules() {
        // Given
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);
        KieContainerBuilderService kieContainerBuilderService = new KieContainerBuilderServiceImpl(droolsRuleRepository);

        DroolsRule dr1 = new DroolsRule();
        dr1.setId("ID");
        dr1.setName("NAME");
        dr1.setRule("""
                package %s;
                                
                rule "NAME"
                when eval(true)
                then System.out.println("OK");
                end
                """.formatted(KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE));

        Mockito.when(droolsRuleRepository.findAll()).thenReturn(Flux.just(dr1));

        // When
        KieBase kieBase = kieContainerBuilderService.buildAll().block();
        Assertions.assertNotNull(kieBase);
        Mockito.verify(droolsRuleRepository).findAll();

        Assertions.assertEquals(1, getRuleBuiltSize(kieBase));
    }

    public static int getRuleBuiltSize(KieBase kieBase) {
        KiePackage kiePackage = kieBase.getKiePackage(KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE);
        return kiePackage != null
                ? kiePackage.getRules().size()
                : 0;
    }
}