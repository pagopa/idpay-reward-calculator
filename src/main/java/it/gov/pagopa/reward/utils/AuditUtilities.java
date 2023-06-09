package it.gov.pagopa.reward.utils;


import it.gov.pagopa.common.utils.AuditLogger;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class AuditUtilities {

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s", AuditLogger.SRCIP);
    private static final String CEF_PATTERN = CEF + " msg={} suser={} cs1Label=TRXIssuer cs1={} cs2Label=TRXAcquirer cs2={} cs3Label=rewards cs3={} cs4Label=rejectionReasons cs4={} cs5Label=initiativeRejectionReasons cs5={}";
    private static final String CEF_CORRELATED_PATTERN = CEF_PATTERN + " cs6Label=correlationId cs6={}";

    public void logCharge(String userId, String trxIssuer, String trxAcquirer, String rewards, String rejectionReasons, String initiativeRejectionReasons) {
        AuditLogger.logAuditString(
                CEF_PATTERN,
                "The charge has been calculated", userId, trxIssuer, trxAcquirer, rewards, rejectionReasons, initiativeRejectionReasons
        );
    }

    public void logRefund(String userId, String trxIssuer, String trxAcquirer, String rewards, String rejectionReasons, String initiativeRejectionReasons, String correlationId) {
        AuditLogger.logAuditString(
                CEF_CORRELATED_PATTERN,
                "The refund has been calculated", userId, trxIssuer, trxAcquirer, rewards, rejectionReasons, initiativeRejectionReasons, correlationId
        );
    }

    public void logExecute(RewardTransactionDTO trx) {
        String rewards = "["+trx.getRewards().values().stream()
                .map(r -> "initiativeId=%s rewardCents=%s".formatted(
                        r.getInitiativeId(), CommonUtilities.euroToCents(r.getAccruedReward())
                ))
                .collect(Collectors.joining(","))+"]";

        String rejectionReasons = "[" + String.join(",", trx.getRejectionReasons()) + "]";
        String initiativeRejectionReasons = "[" +
                trx.getInitiativeRejectionReasons().entrySet().stream()
                        .map(e -> e.getKey() + "=[" + String.join(",", e.getValue()) + "]")
                        .collect(Collectors.joining(","))
                + "]";

        if (OperationType.CHARGE.equals(trx.getOperationTypeTranscoded())) {
            this.logCharge(trx.getUserId(), trx.getIdTrxIssuer(), trx.getIdTrxAcquirer(), rewards, rejectionReasons, initiativeRejectionReasons);
        } else if (OperationType.REFUND.equals(trx.getOperationTypeTranscoded())) {
            this.logRefund(trx.getUserId(), trx.getIdTrxIssuer(), trx.getIdTrxAcquirer(), rewards, rejectionReasons, initiativeRejectionReasons, trx.getCorrelationId());
        }
    }
}