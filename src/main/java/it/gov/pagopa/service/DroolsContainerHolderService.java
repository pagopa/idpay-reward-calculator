package it.gov.pagopa.service;

import org.kie.api.runtime.KieContainer;

/**
 * This component will retrieve the KieContainer configured with all the initiatives
 * */
public interface DroolsContainerHolderService {
    KieContainer getKieContainer();
}
