package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import org.kie.api.runtime.KieContainer;

/**
 * This component will retrieve the KieContainer configured with all the initiatives
 * */
public interface DroolsContainerHolderService {
    KieContainer getRewardRulesKieContainer();
    void setRewardRulesKieContainer(KieContainer kieContainer);
    InitiativeConfig getInitiativeConfig(String initiativeId);
    void setInitiativeConfig(InitiativeConfig initiativeConfig);

}
