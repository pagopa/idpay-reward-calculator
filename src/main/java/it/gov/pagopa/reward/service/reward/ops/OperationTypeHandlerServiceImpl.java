package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@Slf4j
public class OperationTypeHandlerServiceImpl implements OperationTypeHandlerService {

    private final Set<String> chargeOperationTypes;
    private final Set<String> refundOperationTypes;

    private final OperationTypeChargeHandlerService operationTypeChargeHandlerService;
    private final OperationTypeRefundHandlerService operationTypeRefundHandlerService;

    public OperationTypeHandlerServiceImpl(
            @Value("${app.operationType.charge}") Set<String> chargeOperationTypes,
            @Value("${app.operationType.refund}") Set<String> refundOperationTypes, OperationTypeChargeHandlerService operationTypeChargeHandlerService, OperationTypeRefundHandlerService operationTypeRefundHandlerService) {
        this.chargeOperationTypes = chargeOperationTypes;
        this.refundOperationTypes = refundOperationTypes;
        this.operationTypeChargeHandlerService = operationTypeChargeHandlerService;
        this.operationTypeRefundHandlerService = operationTypeRefundHandlerService;
    }

    @Override
    public Mono<TransactionDTO> handleOperationType(TransactionDTO transactionDTO) {
        if(isRefundOperation(transactionDTO)) {
            return operationTypeRefundHandlerService.handleRefundOperation(transactionDTO);
        } else if(isChargeOperation(transactionDTO)) {
            return operationTypeChargeHandlerService.handleChargeOperation(transactionDTO);
        } else {
            log.info("[REWARD] [REWARD_KO] Cannot recognize the operationType: {}", transactionDTO.getOperationType());
            transactionDTO.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_INVALID_OPERATION_TYPE);
            return Mono.just(transactionDTO);
        }
    }

    @Override
    public boolean isChargeOperation(TransactionDTO transactionDTO) {
        return chargeOperationTypes.contains(transactionDTO.getOperationType());
    }

    @Override
    public boolean isRefundOperation(TransactionDTO transactionDTO) {
        return refundOperationTypes.contains(transactionDTO.getOperationType());
    }
}
