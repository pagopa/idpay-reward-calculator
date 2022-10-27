package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanUpdateOutcomeDTO;

public interface HpanUpdateNotifierService {
    boolean notify(HpanUpdateOutcomeDTO hpanUpdateOutcomeDTO);
}