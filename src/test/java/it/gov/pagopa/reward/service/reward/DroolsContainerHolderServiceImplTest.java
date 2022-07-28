package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class DroolsContainerHolderServiceImplTest {

    @Test
    void getKieContainer() {
        // Given
        KieContainerBuilderService kieContainerBuilderService = Mockito.mock(KieContainerBuilderService.class);

        KieContainer kieContainer = Mockito.mock(KieContainer.class);
        Mockito.when(kieContainerBuilderService.buildAll()).thenReturn(Mono.just(kieContainer));

        DroolsContainerHolderService droolsContainerHolderService = new DroolsContainerHolderServiceImpl(kieContainerBuilderService);

        // When
        KieContainer result = droolsContainerHolderService.getRewardRulesKieContainer();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(kieContainer, result);


    }
}