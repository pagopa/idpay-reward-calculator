package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;

/**
 * This component evaluate a {@link HpanInitiativeDTO} with operation type DELETE_INSTRUMENT
 * */
public interface DeleteHpanService {
    HpanInitiatives execute(HpanInitiatives hpanInitiatives, HpanInitiativeDTO hpanInitiativeDTO);
}
