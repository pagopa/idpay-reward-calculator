package it.gov.pagopa.reward.service.lookup.ops;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;

/**
 * This component evaluate a {@link HpanUpdateEvaluateDTO} with operation type DELETE_INSTRUMENT
 * */
public interface DeleteHpanService {
    OnboardedInitiative execute(HpanInitiatives hpanInitiatives, HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO);
}
