package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

/**
 * This component evaluate a single object and save it into DB*/

public interface HpanInitiativesService {
    Mono<HpanInitiatives> hpanInitiativeUpdateInformation(Pair<HpanInitiativeDTO, Mono<HpanInitiatives>> hpanInitiativePair);
}
