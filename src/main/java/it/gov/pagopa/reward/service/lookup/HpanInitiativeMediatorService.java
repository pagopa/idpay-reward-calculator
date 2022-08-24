package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component take a HpanInitiativeDTO and save into DB
 * <ol>
 *     <li>retrieve if hpan is present in DB</li>
 *     <li>valutare la tipologia di operazione</li>
 *     <li>save into DB</li>
 * </ol>*/

public interface HpanInitiativeMediatorService {
    void execute(Flux<Message<HpanInitiativeDTO>> hpanInitiativeDTOFlux);
}

