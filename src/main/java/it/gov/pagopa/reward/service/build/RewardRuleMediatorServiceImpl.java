package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
public class RewardRuleMediatorServiceImpl implements RewardRuleMediatorService {

    private final Duration rewardRulesBuildDelay;

    private final RewardRule2DroolsRuleService rewardRule2DroolsRuleService;
    private final DroolsRuleRepository droolsRuleRepository;
    private final KieContainerBuilderService kieContainerBuilderService;

    private final RewardContextHolderService rewardContextHolderService;

    public RewardRuleMediatorServiceImpl(@Value("${app.reward-rule.build-delay-duration}") String rewardRulesBuildDelay, RewardRule2DroolsRuleService rewardRule2DroolsRuleService, DroolsRuleRepository droolsRuleRepository, KieContainerBuilderService kieContainerBuilderService, RewardContextHolderService rewardContextHolderService) {
        this.rewardRulesBuildDelay=Duration.parse(rewardRulesBuildDelay);
        this.rewardRule2DroolsRuleService = rewardRule2DroolsRuleService;
        this.droolsRuleRepository = droolsRuleRepository;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.rewardContextHolderService = rewardContextHolderService;
    }

    @Override
    public void execute(Flux<InitiativeReward2BuildDTO> initiativeBeneficiaryRuleDTOFlux) {
        initiativeBeneficiaryRuleDTOFlux
                .map(rewardRule2DroolsRuleService) // TODO handle null value due to invalid ruleit.gov.pagopa.reward.service.build.RewardRuleMediatorService
                .flatMap(droolsRuleRepository::save)
                .map(i -> {
                    rewardContextHolderService.setInitiativeConfig(i.getInitiativeConfig());
                        return i;})
                .buffer(rewardRulesBuildDelay)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(rewardContextHolderService::setRewardRulesKieContainer);
    }
}
