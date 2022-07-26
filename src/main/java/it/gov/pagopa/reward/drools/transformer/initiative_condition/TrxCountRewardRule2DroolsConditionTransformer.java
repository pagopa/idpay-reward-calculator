package it.gov.pagopa.reward.drools.transformer.initiative_condition;

import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;

public class TrxCountRewardRule2DroolsConditionTransformer implements InitiativeRewardRule2DroolsConditionTransformer<TrxCountDTO> {
    @Override
    public String apply(TrxCountDTO trxCountDTO) {
        return "TODO";
    } //TODO how to handle min count if we don't count when initiative is not rewarding?
}
