package it.gov.pagopa.reward.utils;


import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Collectors;

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
    private static final String CEF_PATTERN = CEF + " msg={} suser={} cs1Label=TRXIssuer cs1={} cs2Label=TRXAcquirer cs2={} cs3Label=rewards cs3={} cs4Label=rejectionReasons cs4={} cs5Label=initiativeRejectionReasons cs5={}";
    private static final String CEF_CORRELATED_PATTERN = CEF_PATTERN + " cs6Label=correlationId cs6={}";

    private void logAuditString(String pattern, String... parameters) {
        log.info(pattern, (Object[]) parameters);
    }

    public void logCharge(String userId, String trxIssuer, String trxAcquirer, String rewards, String rejectionReasons, String initiativeRejectionReasons) {
        logAuditString(
                CEF_PATTERN,
                "The charge has been calculated", userId, trxIssuer, trxAcquirer, rewards, rejectionReasons, initiativeRejectionReasons
        );
    }

    public void logRefund(String userId, String trxIssuer, String trxAcquirer, String rewards, String rejectionReasons, String initiativeRejectionReasons, String correlationId) {
        logAuditString(
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