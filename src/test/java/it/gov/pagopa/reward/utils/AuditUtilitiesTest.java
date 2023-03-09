package it.gov.pagopa.reward.utils;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.reward.dto.mapper.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.test.fakers.TransactionDroolsDtoFaker;
import it.gov.pagopa.reward.test.utils.MemoryAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class AuditUtilitiesTest {
    private static final String USER = "TEST_USER_ID";
    private static final String CORRELATION_ID = "TEST_CORRELATION_ID";
    private static final String TRX_ISSUER = "TEST_TRX_ISSUER";
    private static final String TRX_ACQUIRER = "TEST_TRX_ACQUIRER";
    private static final String REWARD = "TEST_REWARD";
    private static final String REJECTIONREASONS = "TEST_REJECTIONREASONS";
    private static final String INIATIATIVEREJECTIONREASONS = "TEST_INIATIATIVEREJECTIONREASONS";

    private MemoryAppender memoryAppender;

    private final AuditUtilities auditUtilities = new AuditUtilities();
    private final TransactionDroolsDTO2RewardTransactionMapper mapper = new TransactionDroolsDTO2RewardTransactionMapper();

    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("AUDIT");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @Test
    void testLogExecute_chargeOp(){
        // Given
        RewardTransactionDTO trx = buildTrx(0);

        // When
        auditUtilities.logExecute(trx);

        //Then
        Assertions.assertEquals(
                "CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s msg=The charge has been calculated suser=USERID0 cs1Label=TRXIssuer cs1=IDTRXISSUER0 cs2Label=TRXAcquirer cs2=IDTRXACQUIRER0 cs3Label=rewards cs3=[initiativeId=INITIATIVE3 rewardCents=100] cs4Label=rejectionReasons cs4=[REJECTIONREASON1] cs5Label=initiativeRejectionReasons cs5=[INITIATIVE1=[REJECTIONREASON2],INITIATIVE2=[REJECTIONREASON3,REJECTIONREASON4]]"
                        .formatted(AuditUtilities.SRCIP),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    @Test
    void testLogExecute_refundOp(){
        // Given
        RewardTransactionDTO trx = buildTrx(1);
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.setOperationType("01");

        // When
        auditUtilities.logExecute(trx);

        //Then
        Assertions.assertEquals(
                "CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s msg=The refund has been calculated suser=USERID1 cs1Label=TRXIssuer cs1=IDTRXISSUER1 cs2Label=TRXAcquirer cs2=IDTRXACQUIRER1 cs3Label=rewards cs3=[initiativeId=INITIATIVE3 rewardCents=100] cs4Label=rejectionReasons cs4=[REJECTIONREASON1] cs5Label=initiativeRejectionReasons cs5=[INITIATIVE1=[REJECTIONREASON2],INITIATIVE2=[REJECTIONREASON3,REJECTIONREASON4]] cs6Label=correlationId cs6=CORRELATIONID1"
                        .formatted(AuditUtilities.SRCIP),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    private RewardTransactionDTO buildTrx(int bias) {
        RewardTransactionDTO trx = mapper.apply(TransactionDroolsDtoFaker.mockInstance(bias));
        trx.setRejectionReasons(List.of("REJECTIONREASON1"));
        trx.setInitiativeRejectionReasons(Map.of(
                "INITIATIVE1", List.of("REJECTIONREASON2"),
                "INITIATIVE2", List.of("REJECTIONREASON3", "REJECTIONREASON4")
        ));
        trx.setRewards(Map.of(
                "INITIATIVE3", new Reward("INITIATIVE3", "ORGID", BigDecimal.ONE)
        ));
        return trx;
    }

    @Test
    void logCharge_ok(){
        auditUtilities.logCharge(USER, TRX_ISSUER, TRX_ACQUIRER, REWARD, REJECTIONREASONS, INIATIATIVEREJECTIONREASONS);

        checkCommonFields();

        Assertions.assertEquals(
                "CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s msg=The charge has been calculated suser=%s cs1Label=TRXIssuer cs1=%s cs2Label=TRXAcquirer cs2=%s cs3Label=rewards cs3=%s cs4Label=rejectionReasons cs4=%s cs5Label=initiativeRejectionReasons cs5=%s"
                        .formatted(
                                AuditUtilities.SRCIP,
                                USER,
                                TRX_ISSUER,
                                TRX_ACQUIRER,
                                REWARD,
                                REJECTIONREASONS,
                                INIATIATIVEREJECTIONREASONS
                        ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

    private void checkCommonFields() {
        Assertions.assertTrue(memoryAppender.contains(ch.qos.logback.classic.Level.INFO,USER));
        Assertions.assertTrue(memoryAppender.contains(ch.qos.logback.classic.Level.INFO,TRX_ISSUER));
        Assertions.assertTrue(memoryAppender.contains(ch.qos.logback.classic.Level.INFO,TRX_ACQUIRER));
        Assertions.assertTrue(memoryAppender.contains(ch.qos.logback.classic.Level.INFO,REWARD));

        Assertions.assertEquals(1, memoryAppender.getLoggedEvents().size());
    }

    @Test
    void logRefund_ok(){
        auditUtilities.logRefund(USER, TRX_ISSUER, TRX_ACQUIRER, REWARD, REJECTIONREASONS, INIATIATIVEREJECTIONREASONS, CORRELATION_ID);

        checkCommonFields();
        Assertions.assertTrue(memoryAppender.contains(ch.qos.logback.classic.Level.INFO,CORRELATION_ID));

        Assertions.assertEquals(
                "CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s msg=The refund has been calculated suser=%s cs1Label=TRXIssuer cs1=%s cs2Label=TRXAcquirer cs2=%s cs3Label=rewards cs3=%s cs4Label=rejectionReasons cs4=%s cs5Label=initiativeRejectionReasons cs5=%s cs6Label=correlationId cs6=%s"
                        .formatted(
                                AuditUtilities.SRCIP,
                                USER,
                                TRX_ISSUER,
                                TRX_ACQUIRER,
                                REWARD,
                                REJECTIONREASONS,
                                INIATIATIVEREJECTIONREASONS,
                                CORRELATION_ID
                                ),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }

}
