package it.gov.pagopa.reward.service.reward;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;

class DroolsContainerHolderServiceImplTest {

    @Test
    void getKieContainer() {
        // Given
        DroolsContainerHolderService droolsContainerHolderService = new DroolsContainerHolderServiceImpl();

        // When
        KieContainer result = droolsContainerHolderService.getKieContainer();

        //Then
        Assertions.assertNotNull(result);


    }
}