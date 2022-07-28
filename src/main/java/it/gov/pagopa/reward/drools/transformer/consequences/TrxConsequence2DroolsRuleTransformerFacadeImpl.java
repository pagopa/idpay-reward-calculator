package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardGroupsTrxConsequence2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardLimitsTrxConsequence2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardValueTrxConsequence2DroolsRuleTransformer;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import org.springframework.stereotype.Service;

@Service
public class TrxConsequence2DroolsRuleTransformerFacadeImpl implements TrxConsequence2DroolsRuleTransformerFacade {

    private final RewardValueTrxConsequence2DroolsRuleTransformer rewardValueTrxConsequenceTransformer;
    private final RewardGroupsTrxConsequence2DroolsRuleTransformer rewardGroupsTrxConsequenceTransformer;
    private final RewardLimitsTrxConsequence2DroolsRuleTransformer rewardLimitsTrxConsequenceTransformer;

    public TrxConsequence2DroolsRuleTransformerFacadeImpl(TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade) {
        this.rewardValueTrxConsequenceTransformer = new RewardValueTrxConsequence2DroolsRuleTransformer(trxConsequence2DroolsRewardExpressionTransformerFacade);
        this.rewardGroupsTrxConsequenceTransformer = new RewardGroupsTrxConsequence2DroolsRuleTransformer(trxConsequence2DroolsRewardExpressionTransformerFacade);
        this.rewardLimitsTrxConsequenceTransformer = new RewardLimitsTrxConsequence2DroolsRuleTransformer(trxConsequence2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, InitiativeTrxConsequence trxConsequence) {
        if(trxConsequence == null){
            return "";
        }
        else if(trxConsequence instanceof RewardValueDTO rewardValueDTO){
            return rewardValueTrxConsequenceTransformer.apply(agendaGroup, ruleNamePrefix, rewardValueDTO);
        }
        else if(trxConsequence instanceof RewardGroupsDTO rewardGroupsDTO){
            return rewardGroupsTrxConsequenceTransformer.apply(agendaGroup, ruleNamePrefix, rewardGroupsDTO);
        }
        else if(trxConsequence instanceof RewardLimitsDTO rewardLimitsDTO){
            return rewardLimitsTrxConsequenceTransformer.apply(agendaGroup, ruleNamePrefix, rewardLimitsDTO);
        }

        throw new IllegalStateException("InitiativeTrxConsequence not handled: %s".formatted(trxConsequence.getClass().getName()));
    }
}
