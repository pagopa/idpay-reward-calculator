package it.gov.pagopa.reward.utils;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;
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
    private static final String CORRELATION_ID = "cs1Label=correlationId cs1=";
    private static final String TRX_ISSUER = "cs2Label=TRXIssuer cs2=";
    private static final String TRX_ACQUIRER = "cs3Label=TRXAcquirer cs3=";
    private static final String REWARD = "cs4Label=reward cs4=";
    final Logger logger = Logger.getLogger("AUDIT");


    private String buildLog(String eventLog, String userId, String trxIssuer, String trxAcquirer, String reward) {
        return CEF + MSG + eventLog + " " + USER + userId + " " + TRX_ISSUER + trxIssuer + " " + TRX_ACQUIRER + trxAcquirer + " " + REWARD + reward;
    }

    public void logCharge(String userId, String trxIssuer, String trxAcquirer, String reward) {
        String testLog = this.buildLog("The charge has been calculated", userId, trxIssuer, trxAcquirer, reward);
        logger.info(testLog);
    }

    public void logRefund(String userId, String trxIssuer, String trxAcquirer, String reward, String correlationId) {
        String testLog = this.buildLog("The refund has been calculated", userId, trxIssuer, trxAcquirer, reward) + " " + CORRELATION_ID + correlationId;
        logger.info(testLog);
    }

}