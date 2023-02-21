package it.gov.pagopa.reward.utils;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Collectors;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {
    public static final String SRCIP;

    static {
        String srcIp;
        try {
            srcIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("Cannot determine the ip of the current host", e);
            srcIp="UNKNOWN";
        }

        SRCIP = srcIp;
    }

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s", SRCIP);

    private void logAuditString(String userId) {
        log.info(userId);
    }

    private StringBuilder buildBaseAuditLog(String eventLog, String userId, String trxIssuer, String trxAcquirer, String rewards) {
        return new StringBuilder(CEF)
                .append(" msg=")
                .append(eventLog)
                .append(" suser=")
                .append(userId)
                .append(" cs1Label=TRXIssuer cs1=")
                .append(trxIssuer)
                .append(" cs2Label=TRXAcquirer cs2=")
                .append(trxAcquirer)
                .append(" cs3Label=rewards cs3=")
                .append(rewards);
    }

    public void logCharge(String userId, String trxIssuer, String trxAcquirer, String rewards) {
        logAuditString(
                this.buildBaseAuditLog("The charge has been calculated", userId, trxIssuer, trxAcquirer, rewards).toString()
        );
    }

    public void logRefund(String userId, String trxIssuer, String trxAcquirer, String rewards, String correlationId) {
        logAuditString(
                this.buildBaseAuditLog("The refund has been calculated", userId, trxIssuer, trxAcquirer, rewards)
                        .append(" cs4Label=correlationId cs4=")
                        .append(correlationId)
                        .toString()
        );
    }

    public void logExecute(TransactionDTO payload, RewardTransactionDTO trx) {
        String rewards = trx.getRewards().values().stream()
                .map(r -> "rewardCents=%s initiativeId=%s".formatted(
                        Utils.euroToCents(r.getAccruedReward()), r.getInitiativeId()
                ))
                .collect(Collectors.joining(" "));

        if (OperationType.CHARGE.equals(payload.getOperationTypeTranscoded())) {
            this.logCharge(payload.getUserId(), payload.getIdTrxIssuer(), payload.getIdTrxAcquirer(), rewards);
        } else if (OperationType.REFUND.equals(payload.getOperationTypeTranscoded())) {
            this.logRefund(payload.getUserId(), payload.getIdTrxIssuer(), payload.getIdTrxAcquirer(), rewards, payload.getCorrelationId());
        }
    }
}