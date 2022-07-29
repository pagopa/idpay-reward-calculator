package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import org.kie.api.runtime.KieContainer;

/**
 * This component will retrieve the rewards' rules kieContainer
 * It will also update the cached version when new rules arrives
 * */
public interface DroolsContainerHolderService {
    KieContainer getRewardRulesKieContainer();
    void setRewardRulesKieContainer(KieContainer kieContainer);
    InitiativeConfig getInitiativeConfig(String initiativeId);
    void setInitiativeConfig(InitiativeConfig initiativeConfig);

}
