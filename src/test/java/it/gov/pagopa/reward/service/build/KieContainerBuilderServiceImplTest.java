package it.gov.pagopa.reward.service.build;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

        DroolsRule droolsRule1 = Mockito.mock(DroolsRule.class);

        Mockito.when(droolsRuleRepository.findAll()).thenReturn(Flux.just(droolsRule1));

        // When
        kieContainerBuilderService.buildAll().subscribe(k -> {
            Assertions.assertNotNull(k);
            Mockito.verify(droolsRuleRepository).findAll();
        });

    }
}