package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;

public class TrxCountTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<TrxCountDTO> {
    @Override
    public String apply(TrxCountDTO trxCountDTO) {
        return "true"; // TODO
    } //TODO how to handle min count if we don't count when initiative is not rewarding?
}
