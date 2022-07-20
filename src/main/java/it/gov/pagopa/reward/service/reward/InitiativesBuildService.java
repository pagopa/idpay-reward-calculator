package it.gov.pagopa.reward.service.reward;

import org.kie.api.runtime.KieContainer;

/**
 * this component retrieve all rules and will build the kieContainer*/
public interface InitiativesBuildService {
    KieContainer buildAllRules();
}