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
    private static final String CEF_BASE_PATTERN = CEF + " msg={}";
    private static final String CEF_BASE_USER_PATTERN = CEF_BASE_PATTERN + " suser={}";
    private static final String CEF_PATTERN = CEF_BASE_USER_PATTERN + " cs1Label=TRXIssuer cs1={} cs2Label=TRXAcquirer cs2={} cs3Label=rewards cs3={} cs4Label=rejectionReasons cs4={} cs5Label=initiativeRejectionReasons cs5={}";
    private static final String CEF_CORRELATED_PATTERN = CEF_PATTERN + " cs6Label=correlationId cs6={}";
    private static final String CEF_PATTERN_DELETE = CEF_BASE_PATTERN + " cs1Label=initiativeId cs1={}";
    private static final String CEF_BENEFICIARY_DELETE_PATTERN = CEF_PATTERN_DELETE + " cs2Label=beneficiaryId cs2={}";
    private static final String CEF_INSTRUMENTS_COUNT_DELETE_PATTERN = CEF_PATTERN_DELETE + " cs2Label=numberInstruments cs2={}";
    private static final String CEF_INSTRUMENTS_DELETE_PATTERN = CEF_BASE_USER_PATTERN + " cs1Label=hpan cs1={}";
    private static final String CEF_TRANSACTIONS_COUNT_DELETE_PATTERN = CEF_PATTERN_DELETE + " cs2Label=numberTransactions cs2={}";


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

    //region delete
    public void logDeletedRewardDroolRule(String initiativeId) {
        AuditLogger.logAuditString(
                CEF_PATTERN_DELETE,
                "Reward rule deleted.", initiativeId
        );
    }

    public void logDeletedEntityCounters(String initiativeId, String entityId) {
        AuditLogger.logAuditString(
                CEF_BENEFICIARY_DELETE_PATTERN,
                "Entity counter deleted.", initiativeId, entityId
        );
    }

    public void logDeletedHpanInitiative(String initiativeId, Long deletedHpanInitiative) {
        AuditLogger.logAuditString(
                CEF_INSTRUMENTS_COUNT_DELETE_PATTERN,
                "Payment instruments deleted.", initiativeId, deletedHpanInitiative.toString()
        );
    }

    public void logDeletedTransaction(String initiativeId, Long deletedTransactions) {
        AuditLogger.logAuditString(
                CEF_TRANSACTIONS_COUNT_DELETE_PATTERN,
                "Transactions deleted.", initiativeId, deletedTransactions.toString()
        );
    }

    public void logDeletedHpan(String paymentInstrument, String userId) {
        AuditLogger.logAuditString(
                CEF_INSTRUMENTS_DELETE_PATTERN,
                "Payment instruments without any initiative associate deleted.", userId, paymentInstrument
        );
    }

    public void logDeletedTransactionForUser(String userId) {
        AuditLogger.logAuditString(
                CEF_BASE_USER_PATTERN,
                "Transactions without any initiative associate deleted.", userId
        );
    }

    //endregion
}