package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;

/**
 * This component evaluate a single object and save it into DB*/

public interface HpanInitiativesService {
   HpanInitiatives evaluate(HpanInitiativeDTO hpanInitiativeDTO, HpanInitiatives hpanRetrieved);
}
