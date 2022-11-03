package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OperationTypeChargeHandlerServiceImpl implements OperationTypeChargeHandlerService {
    @Override
    public Mono<TransactionDTO> handleChargeOperation(TransactionDTO trx) {
        log.debug("[REWARD] Recognized a CHARGE operation {}", trx.getId());
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setTrxChargeDate(trx.getTrxDate());
        trx.setEffectiveAmount(trx.getAmount());

        return Mono.just(trx);
    }
}
