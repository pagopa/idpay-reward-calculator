package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.TransactionDTO;
import reactor.core.publisher.Mono;

public interface OperationTypeHandlerService {
    Mono<TransactionDTO> handleOperationType(TransactionDTO transactionDTO);
}
