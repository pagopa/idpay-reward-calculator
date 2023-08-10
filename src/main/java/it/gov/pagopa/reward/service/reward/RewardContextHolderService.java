package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import org.kie.api.KieBase;
import reactor.core.publisher.Mono;

/**
 * This component will retrieve the rewards' rules kieContainer
 * It will also update the cached version when new rules arrives
 * */
public interface RewardContextHolderService {
    KieBase getRewardRulesKieBase();
    void setRewardRulesKieBase(KieBase kieBase);
    Mono<InitiativeConfig> getInitiativeConfig(String initiativeId);
    void setInitiativeConfig(InitiativeConfig initiativeConfig);
    Mono<KieBase> refreshKieContainerCacheMiss();

}
