package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import reactor.core.publisher.Mono;

public interface OperationTypeChargeHandlerService {
    Mono<TransactionDTO> handleChargeOperation(TransactionDTO trx);
}
