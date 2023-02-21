package it.gov.pagopa.reward.utils;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class AuditUtilities {
    private static String SRCIP;

    static {
        try {
            SRCIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error(e.getMessage());
        }
    }

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s", SRCIP);
    private static final String MSG = " msg=";
    private static final String USER = "suser=";
    private static final String TRX_ISSUER = "cs1Label=TRXIssuer cs1=";
    private static final String TRX_ACQUIRER = "cs2Label=TRXAcquirer cs2=";
    private static final String REWARDS = "cs3Label=rewards cs3=";
    private static final String CORRELATION_ID = "cs4Label=correlationId cs4=";
    final Logger logger = Logger.getLogger("AUDIT");


    private String buildLog(String eventLog, String userId, String trxIssuer, String trxAcquirer, String rewards) {
        return CEF + MSG + eventLog + " " + USER + userId + " " + TRX_ISSUER + trxIssuer + " " + TRX_ACQUIRER + trxAcquirer + " " + REWARDS + rewards;
    }

    public void logCharge(String userId, String trxIssuer, String trxAcquirer, String rewards) {
        String testLog = this.buildLog("The charge has been calculated", userId, trxIssuer, trxAcquirer, rewards);
        logger.info(testLog);
    }

    public void logRefund(String userId, String trxIssuer, String trxAcquirer, String rewards, String correlationId) {
        String testLog = this.buildLog("The refund has been calculated", userId, trxIssuer, trxAcquirer, rewards) + " " + CORRELATION_ID + correlationId;
        logger.info(testLog);
    }

    public void logExecute(TransactionDTO payload, RewardTransactionDTO r) {
        String rewards = r.getRewards().entrySet().stream().collect(Collectors.toMap(entry ->
                "reward=".concat(entry.getValue().getProvidedReward().toString())
                        .concat(" initiativeId"), Map.Entry::getKey)).toString();
        if (OperationType.CHARGE.equals(payload.getOperationTypeTranscoded())) {
            this.logCharge(payload.getUserId(), payload.getIdTrxIssuer(), payload.getIdTrxAcquirer(), rewards);
        } else if (OperationType.REFUND.equals(payload.getOperationTypeTranscoded())) {
            this.logRefund(payload.getUserId(), payload.getIdTrxIssuer(), payload.getIdTrxAcquirer(), rewards, payload.getCorrelationId());
        }
    }
}