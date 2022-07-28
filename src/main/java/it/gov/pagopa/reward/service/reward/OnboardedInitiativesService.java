package it.gov.pagopa.reward.service.reward;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * This component will retrieve initiatives to which the input hpan has been enabled
 * */
public interface OnboardedInitiativesService {
    List<String> getInitiatives(String hpan, OffsetDateTime trxDate);
}
