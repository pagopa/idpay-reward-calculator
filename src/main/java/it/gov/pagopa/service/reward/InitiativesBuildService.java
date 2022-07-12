package it.gov.pagopa.service.reward;

import org.kie.api.runtime.KieContainer;

/**
 * this component retrieve all rules and will build the kieContainer*/
public interface InitiativesBuildService {
    KieContainer buildAllRules();
}
