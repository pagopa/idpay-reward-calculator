package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OperationTypeRefundHandlerServiceImpl implements OperationTypeRefundHandlerService {

    private final TransactionProcessedRepository transactionProcessedRepository;

    public OperationTypeRefundHandlerServiceImpl(TransactionProcessedRepository transactionProcessedRepository) {
        this.transactionProcessedRepository = transactionProcessedRepository;
    }

    @Override
    public Mono<TransactionDTO> handleRefundOperation(TransactionDTO trx) {
        log.debug("[REWARD] Recognized a REFUND operation {}", trx.getId());
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        if (!StringUtils.hasText(trx.getCorrelationId())) {
            trx.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_INVALID_REFUND);
            return Mono.just(trx);
        } else {
            return handleRefund(trx);
        }
    }

    private Mono<TransactionDTO> handleRefund(TransactionDTO trx) {
        TransactionProcessed query = TransactionProcessed.builder()
                .acquirerId(trx.getAcquirerId())
                .correlationId(trx.getCorrelationId())
                .build();
        return transactionProcessedRepository.findAll(Example.of(query))
                .collectList()
                .map(pastTrxs -> evaluatePastTransactions(trx, pastTrxs));
    }

    private TransactionDTO evaluatePastTransactions(TransactionDTO trx, List<TransactionProcessed> pastTrxs) {
        log.trace("[REWARD] Retrieved correlated trxs {} {}", pastTrxs.size(), trx.getId());

        TransactionProcessed trxCharge = null;
        BigDecimal effectiveAmount = trx.getAmount().negate();
        Map<String, BigDecimal> pastRewards = new HashMap<>();

        if (!pastTrxs.isEmpty()) {
            for (TransactionProcessed pt : pastTrxs) {
                if (pt.getOperationTypeTranscoded().equals(OperationType.CHARGE)) {
                    trxCharge = pt;
                    effectiveAmount = effectiveAmount.add(pt.getAmount());
                } else {
                    effectiveAmount = effectiveAmount.subtract(pt.getAmount());
                }

                reduceRewards(pastRewards, pt);
            }
        }

        if (trxCharge == null) {
            trx.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH));
        } else {
            trx.setTrxChargeDate(OffsetDateTime.of(trxCharge.getTrxChargeDate(), RewardConstants.ZONEID.getRules().getOffset(trxCharge.getTrxChargeDate())));
            trx.setEffectiveAmount(effectiveAmount);
            trx.setRefundInfo(new RefundInfo());
            trx.getRefundInfo().setPreviousTrxs(pastTrxs);
            trx.getRefundInfo().setPreviousRewards(pastRewards);
        }

        return trx;
    }

    private void reduceRewards(Map<String, BigDecimal> pastRewards, TransactionProcessed pt) {
        pt.getRewards().forEach((initiativeId, r) -> pastRewards.compute(initiativeId, (k, acc) -> {
            if (acc != null) {
                final BigDecimal sum = r.getAccruedReward().add(acc);
                if (BigDecimal.ZERO.compareTo(sum) != 0) {
                    return sum;
                } else {
                    return null;
                }
            } else {
                return r.getAccruedReward().setScale(2, RoundingMode.UNNECESSARY);
            }
        }));
    }
}
