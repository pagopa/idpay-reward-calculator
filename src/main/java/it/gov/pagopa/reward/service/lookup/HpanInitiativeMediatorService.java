package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

/**
 * This component take a HpanInitiativeDTO and save into DB
 * <ol>
 *     <li>retrieve if hpan is present in DB</li>
 *     <li>evaluate message</li>
 *     <li>save into DB</li>
 * </ol>*/

public interface HpanInitiativeMediatorService {
    void execute(Flux<Message<String>> hpanInitiativeDTOFlux);
    Flux<String> evaluate(HpanInitiativeBulkDTO hpanInitiativeBulkDTO, LocalDateTime evaluationDate);
}

