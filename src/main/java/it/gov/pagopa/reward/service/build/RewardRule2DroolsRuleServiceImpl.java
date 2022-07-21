package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.AnyOfInitiativeRewardRule;
import it.gov.pagopa.reward.model.DroolsRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

@Service
@Slf4j
public class RewardRule2DroolsRuleServiceImpl implements RewardRule2DroolsRuleService{

    private final boolean onlineSyntaxCheck;

    private final KieContainerBuilderService builderService;

    public RewardRule2DroolsRuleServiceImpl(@Value("${app.reward-rule.online-syntax-check}") boolean onlineSyntaxCheck, KieContainerBuilderService builderService) {
        this.onlineSyntaxCheck = onlineSyntaxCheck;
        this.builderService = builderService;
    }
    @Override
    public DroolsRule apply(InitiativeReward2BuildDTO initiative) {
        log.info("Building inititative having id: %s".formatted(initiative.getInitiativeId()));

        try {
            DroolsRule out = new DroolsRule();
            out.setId(initiative.getInitiativeId());
            out.setName(String.format("%s-%s", initiative.getInitiativeId(), initiative.getInitiativeName()));

            out.setRule("""
                    package %s;
                                        
                    %s
                    """.formatted(
                    KieContainerBuilderServiceImpl.rulesBuiltPackage,
                    "TODO")//initiative.getRewardRule().stream().map(c -> initiativeRewardRuleBuild(out.getId(), out.getName(), c)).collect(Collectors.joining("\n\n")))
            );

            if(onlineSyntaxCheck){
                log.debug("Checking if the rule has valid syntax. id: %s".formatted(initiative.getInitiativeId()));
                builderService.build(Flux.just(out)).block(); // TODO handle if it goes to exception due to error
            }

            log.debug("Conversion into drools rule completed; storing it. id: %s".formatted(initiative.getInitiativeId()));
            return out;
        } catch (RuntimeException e){
            log.error("Something gone wrong while building initiative %s".formatted(initiative.getInitiativeId()), e);
            return null;
        }
    }

    /*private String initiativeRewardRuleBuild(String initiativeId, String ruleName, AnyOfInitiativeRewardRule rewardRule){
        return """
                rule "%s"
                agenda-group "%s"
                when $onboarding: %s(%s)
                then $onboarding.getOnboardingRejectionReasons().add("AUTOMATED_CRITERIA_%s_FAIL");
                end
                """.formatted(
                ruleName + "-" + rewardRule.getCode(),
                initiativeId,
                OnboardingDroolsDTO.class.getName(),
                extraFilter2DroolsTransformerFacade.apply(automatedCriteria2ExtraFilter(rewardRule, criteriaCodeConfig), OnboardingDTO.class, null),
                rewardRule.getCode()
        );
    }*/
}
