package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import reactor.core.publisher.Mono;

public interface CancelTrxSynchronousService {
    Mono<SynchronousTransactionResponseDTO> cancelTransaction(String trxId);
}
