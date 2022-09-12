package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
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
    private final Set<String> reversalOperationTypes;

    public OperationTypeHandlerServiceImpl(
            @Value("${app.operationType.charge}") Set<String> chargeOperationTypes,
            @Value("${app.operationType.reversal}") Set<String> reversalOperationTypes) {
        this.chargeOperationTypes = chargeOperationTypes;
        this.reversalOperationTypes = reversalOperationTypes;
    }

    @Override
    public Mono<TransactionDTO> handleOperationType(TransactionDTO transactionDTO) {
        if(reversalOperationTypes.contains(transactionDTO.getOperationType())) {
            log.debug("[REWARD] Recognized a REVERSAL operation");
            transactionDTO.setOperationTypeTranscoded(OperationType.REVERSAL);
            return Mono.empty();// TODO
        } else {
            if(chargeOperationTypes.contains(transactionDTO.getOperationType())) {
                log.debug("[REWARD] Recognized a CHARGE operation");
                transactionDTO.setOperationTypeTranscoded(OperationType.CHARGE);
                transactionDTO.setTrxChargeDate(transactionDTO.getTrxDate());
                transactionDTO.setEffectiveAmount(transactionDTO.getAmount());
            } else {
                log.error("[REWARD] [REWARD_KO] Cannot recognize the operationType: {}", transactionDTO.getOperationType());
                transactionDTO.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_INVALID_OPERATION_TYPE);
            }
            return Mono.just(transactionDTO);
        }
    }
}
