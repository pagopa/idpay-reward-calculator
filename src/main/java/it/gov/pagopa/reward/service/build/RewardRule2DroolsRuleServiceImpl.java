package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsRuleTransformerFacade;
import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRuleTransformerFacade;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.mapper.InitiativeReward2BuildDTO2ConfigMapper;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class RewardRule2DroolsRuleServiceImpl implements RewardRule2DroolsRuleService {

    private final boolean onlineSyntaxCheck;

    private final KieContainerBuilderService builderService;
    private final TrxCondition2DroolsRuleTransformerFacade trxCondition2DroolsRuleTransformerFacade;
    private final TrxConsequence2DroolsRuleTransformerFacade trxConsequence2DroolsRuleTransformerFacade;

    private final InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper;

    public RewardRule2DroolsRuleServiceImpl(
            @Value("${app.reward-rule.online-syntax-check}") boolean onlineSyntaxCheck,
            KieContainerBuilderService builderService,
            TrxCondition2DroolsRuleTransformerFacade trxCondition2DroolsRuleTransformerFacade, TrxConsequence2DroolsRuleTransformerFacade trxConsequence2DroolsRuleTransformerFacade,
            InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper) {
        this.onlineSyntaxCheck = onlineSyntaxCheck;
        this.builderService = builderService;
        this.trxCondition2DroolsRuleTransformerFacade = trxCondition2DroolsRuleTransformerFacade;
        this.trxConsequence2DroolsRuleTransformerFacade = trxConsequence2DroolsRuleTransformerFacade;
        this.initiativeReward2BuildDTO2ConfigMapper = initiativeReward2BuildDTO2ConfigMapper;
    }

    @Override
    public DroolsRule apply(InitiativeReward2BuildDTO initiative) {
        log.info("Building initiative having id: %s".formatted(initiative.getInitiativeId()));

        DroolsRule out = new DroolsRule();
        out.setId(initiative.getInitiativeId());
        out.setName(String.format("%s-%s", initiative.getInitiativeId(), initiative.getInitiativeName()));

        out.setRule("""
                package %s;
                %s
                """.formatted(
                KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE,
                buildRules(out.getId(), out.getName(), initiative))
        );
        out.setUpdateDate(LocalDateTime.now());

        if (onlineSyntaxCheck) {
            log.debug("Checking if the rule has valid syntax. id: %s".formatted(initiative.getInitiativeId()));
            builderService.build(Flux.just(out)).block();
        }

        out.setInitiativeConfig(initiativeReward2BuildDTO2ConfigMapper.apply(initiative));

        log.debug("Conversion into drools rule completed; storing it. id: %s".formatted(initiative.getInitiativeId()));
        return out;
    }

    private String buildRules(String initiativeId, String ruleNamePrefix, InitiativeReward2BuildDTO initiative) {
        StringBuilder initiativeRulesBuilder = new StringBuilder();

        buildRuleConditions(initiativeId, ruleNamePrefix, initiative, initiativeRulesBuilder);
        buildRuleConsequences(initiativeId, ruleNamePrefix, initiative, initiativeRulesBuilder);

        return initiativeRulesBuilder.toString();
    }

    private void buildRuleConditions(String initiativeId, String ruleNamePrefix, InitiativeReward2BuildDTO initiative, StringBuilder initiativeRulesBuilder) {
        InitiativeTrxConditions trxRules = initiative.getTrxRule();
        if (trxRules != null) {
            initiativeRulesBuilder.append(trxCondition2DroolsRuleTransformerFacade.apply(initiativeId, ruleNamePrefix, trxRules.getDaysOfWeek()));
            initiativeRulesBuilder.append(trxCondition2DroolsRuleTransformerFacade.apply(initiativeId, ruleNamePrefix, trxRules.getMccFilter()));

            if (!CollectionUtils.isEmpty(trxRules.getRewardLimits())) {
                List<RewardLimitsDTO> rewardLimits = trxRules.getRewardLimits();
                for (RewardLimitsDTO rewardLimit : rewardLimits) {
                    initiativeRulesBuilder.append(trxCondition2DroolsRuleTransformerFacade.apply(initiativeId, ruleNamePrefix, rewardLimit));
                }
            }

            initiativeRulesBuilder.append(trxCondition2DroolsRuleTransformerFacade.apply(initiativeId, ruleNamePrefix, trxRules.getThreshold()));
            initiativeRulesBuilder.append(trxCondition2DroolsRuleTransformerFacade.apply(initiativeId, ruleNamePrefix, trxRules.getTrxCount()));

            if (initiative.getRewardRule() instanceof RewardGroupsDTO rewardGroupsDTO) {
                initiativeRulesBuilder.append(trxCondition2DroolsRuleTransformerFacade.apply(initiativeId, ruleNamePrefix, rewardGroupsDTO));
            }
        }
    }

    private void buildRuleConsequences(String initiativeId, String ruleNamePrefix, InitiativeReward2BuildDTO initiative, StringBuilder initiativeRulesBuilder) {
        initiativeRulesBuilder.append(trxConsequence2DroolsRuleTransformerFacade.apply(initiativeId, initiative.getOrganizationId(), ruleNamePrefix, initiative.getRewardRule()));
        if (!CollectionUtils.isEmpty(initiative.getTrxRule().getRewardLimits())) {
            initiative.getTrxRule().getRewardLimits().forEach(rl ->
                    initiativeRulesBuilder.append(trxConsequence2DroolsRuleTransformerFacade.apply(initiativeId, initiative.getOrganizationId(), ruleNamePrefix, rl))
            );
        }
        if(initiative.getTrxRule().getTrxCount()!=null){
            initiativeRulesBuilder.append(trxConsequence2DroolsRuleTransformerFacade.apply(initiativeId, initiative.getOrganizationId(), ruleNamePrefix, initiative.getTrxRule().getTrxCount()));
        }
    }

}
