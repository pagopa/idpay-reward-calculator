package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.reward.DroolsContainerHolderService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class RewardRuleMediatorServiceImpl implements RewardRuleMediatorService {

    private final RewardRule2DroolsRule rewardRule2DroolsRule;
    private final DroolsRuleRepository droolsRuleRepository;
    private final KieContainerBuilderService kieContainerBuilderService;

    private final DroolsContainerHolderService droolsContainerHolderService;

    public RewardRuleMediatorServiceImpl(RewardRule2DroolsRule rewardRule2DroolsRule, DroolsRuleRepository droolsRuleRepository, KieContainerBuilderService kieContainerBuilderService, DroolsContainerHolderService droolsContainerHolderService) {
        this.rewardRule2DroolsRule = rewardRule2DroolsRule;
        this.droolsRuleRepository = droolsRuleRepository;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.droolsContainerHolderService = droolsContainerHolderService;
    }

    @Override
    public void execute(Flux<InitiativeReward2BuildDTO> initiativeBeneficiaryRuleDTOFlux) {
        rewardRule2DroolsRule.apply(initiativeBeneficiaryRuleDTOFlux) // TODO handle null value due to invalid ruleit.gov.pagopa.reward.service.build.RewardRuleMediatorService
                .flatMap(droolsRuleRepository::save)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(droolsContainerHolderService::setKieContainer);
    }
}
