package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardGroupsTrxConsequence2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardLimitsTrxConsequence2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardValueTrxConsequence2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.TrxCountTrxConsequence2DroolsExpressionTransformer;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import org.springframework.stereotype.Service;

@Service
public class TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl implements TrxConsequence2DroolsRewardExpressionTransformerFacade {

    private final RewardValueTrxConsequence2DroolsExpressionTransformer rewardValueTrxConsequenceTransformer;
    private final RewardGroupsTrxConsequence2DroolsExpressionTransformer rewardGroupsTrxConsequenceTransformer;
    private final RewardLimitsTrxConsequence2DroolsExpressionTransformer rewardLimitsTrxConsequenceTransformer;
    private final TrxCountTrxConsequence2DroolsExpressionTransformer trxCountTrxConsequenceTransformer;

    public TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl() {
        this.rewardValueTrxConsequenceTransformer = new RewardValueTrxConsequence2DroolsExpressionTransformer();
        this.rewardGroupsTrxConsequenceTransformer = new RewardGroupsTrxConsequence2DroolsExpressionTransformer();
        this.rewardLimitsTrxConsequenceTransformer = new RewardLimitsTrxConsequence2DroolsExpressionTransformer();
        this.trxCountTrxConsequenceTransformer = new TrxCountTrxConsequence2DroolsExpressionTransformer();
    }

    @Override
    public String apply(String initiativeId, InitiativeTrxConsequence trxConsequence) {
        if(trxConsequence == null){
            return "";
        }
        else if(trxConsequence instanceof RewardValueDTO rewardValueDTO){
            return rewardValueTrxConsequenceTransformer.apply(initiativeId, rewardValueDTO);
        }
        else if(trxConsequence instanceof RewardGroupsDTO rewardGroupsDTO){
            return rewardGroupsTrxConsequenceTransformer.apply(initiativeId, rewardGroupsDTO);
        }
        else if(trxConsequence instanceof RewardLimitsDTO rewardLimitsDTO){
            return rewardLimitsTrxConsequenceTransformer.apply(initiativeId, rewardLimitsDTO);
        }
        else if(trxConsequence instanceof TrxCountDTO trxCountDTO){
            return trxCountTrxConsequenceTransformer.apply(initiativeId, trxCountDTO);
        }

        throw new IllegalStateException("InitiativeTrxConsequence not handled: %s".formatted(trxConsequence.getClass().getName()));
    }
}
