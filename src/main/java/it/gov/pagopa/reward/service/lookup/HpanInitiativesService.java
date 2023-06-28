package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;

/**
 * This component evaluate a single object and save it into DB*/

public interface HpanInitiativesService {
   OnboardedInitiative evaluate(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO, HpanInitiatives hpanRetrieved, boolean recessFlow);
}
