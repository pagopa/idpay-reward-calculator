package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
        return transactionProcessedRepository.findByAcquirerIdAndCorrelationId(trx.getAcquirerId(), trx.getCorrelationId())
                .collectList()
                .map(pastTrxs -> evaluatePastTransactions(trx, pastTrxs));
    }

    private TransactionDTO evaluatePastTransactions(TransactionDTO trx, List<BaseTransactionProcessed> pastTrxs) {
        log.trace("[REWARD] Retrieved correlated trxs {} {}", pastTrxs.size(), trx.getId());

        TransactionProcessed trxCharge = null;
        Long effectiveAmountCents = - trx.getAmountCents();
        Map<String, RefundInfo.PreviousReward> pastRewards = new HashMap<>();

        List<TransactionProcessed> pastElabTrxs = null;

        if (!pastTrxs.isEmpty()) {
            pastElabTrxs = new ArrayList<>(pastTrxs.size());

            for (BaseTransactionProcessed pt : pastTrxs) {
                // Considering only processed transactions
                if(pt instanceof TransactionProcessed pastProcessed) {
                    if (pastProcessed.getOperationTypeTranscoded().equals(OperationType.CHARGE)) {
                        trxCharge = pastProcessed;
                        effectiveAmountCents = effectiveAmountCents + pastProcessed.getAmountCents();
                    } else {
                        effectiveAmountCents = effectiveAmountCents - pastProcessed.getAmountCents();
                    }

                    reduceRewards(pastRewards, pastProcessed);

                    pastElabTrxs.add(pastProcessed);
                }

            }
        }

        if (trxCharge == null) {
            trx.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH));
        } else {
            trx.setTrxChargeDate(readChargeDate(trxCharge));
            trx.setEffectiveAmountCents(effectiveAmountCents);
            trx.setRefundInfo(new RefundInfo());
            trx.getRefundInfo().setPreviousTrxs(pastElabTrxs);
            trx.getRefundInfo().setPreviousRewards(pastRewards);
        }

        return trx;
    }

    private static OffsetDateTime readChargeDate(TransactionProcessed trxCharge) {
        LocalDateTime trxChargeLocalDateTime = trxCharge.getTrxChargeDate();
        return OffsetDateTime.of(trxChargeLocalDateTime, CommonConstants.ZONEID.getRules().getOffset(trxChargeLocalDateTime));
    }

    private void reduceRewards(Map<String, RefundInfo.PreviousReward> pastRewards, BaseTransactionProcessed pt) {
        pt.getRewards().forEach((initiativeId, r) -> pastRewards.compute(initiativeId, (k, acc) -> {
            if (acc != null) {
                final Long sum = r.getAccruedRewardCents() + acc.getAccruedRewardCents();
                acc.setAccruedRewardCents(sum);
                return acc;
            } else {
                return new RefundInfo.PreviousReward(r.getInitiativeId(), r.getOrganizationId(), r.getAccruedRewardCents());
            }
        }));
    }
}
