package it.gov.pagopa.service.reward;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * This component will retrieve initiatives to which the input hpan has been enabled
 * */
public interface InitiativesService {
    List<String> getInitiatives(String hpan, OffsetDateTime trxDate);
}
