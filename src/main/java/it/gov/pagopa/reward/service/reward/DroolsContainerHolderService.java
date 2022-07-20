package it.gov.pagopa.reward.service.reward;

import org.kie.api.runtime.KieContainer;

/**
 * This component will retrieve the KieContainer configured with all the initiatives
 * */
public interface DroolsContainerHolderService {
    KieContainer getKieContainer();

    void setKieContainer(KieContainer kieContainer);
}
