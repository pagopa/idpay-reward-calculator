package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import reactor.core.publisher.Mono;

public interface OperationTypeHandlerService {
    Mono<TransactionDTO> handleOperationType(TransactionDTO transactionDTO);

    boolean isChargeOperation(TransactionDTO transactionDTO);

    boolean isRefundOperation(TransactionDTO transactionDTO);
}
