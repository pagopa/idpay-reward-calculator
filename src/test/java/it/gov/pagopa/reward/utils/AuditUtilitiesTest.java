package it.gov.pagopa.reward.utils;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

class AuditUtilitiesTest {
    private static final String USER = "TEST_USER_ID";
    private static final String CORRELATION_ID = "TEST_CORRELATION_ID";
    private static final String TRX_ISSUER = "TEST_TRX_ISSUER";
    private static final String TRX_ACQUIRER = "TEST_TRX_ACQUIRER";
    private static final String REWARD = "TEST_REWARD";

    private MemoryAppender memoryAppender;

    private final AuditUtilities auditUtilities = new AuditUtilities();

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
    void logCharge_ok(){
        auditUtilities.logCharge(USER, TRX_ISSUER, TRX_ACQUIRER, REWARD);

        checkCommonFields();

        Assertions.assertEquals(
                "CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s msg=The charge has been calculated suser=%s cs1Label=TRXIssuer cs1=%s cs2Label=TRXAcquirer cs2=%s cs3Label=rewards cs3=%s"
                        .formatted(
                                AuditUtilities.SRCIP,
                                USER,
                                TRX_ISSUER,
                                TRX_ACQUIRER,
                                REWARD
                        ),
                memoryAppender.getLoggedEvents().get(0).getMessage()
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
        auditUtilities.logRefund(USER, TRX_ISSUER, TRX_ACQUIRER, REWARD, CORRELATION_ID);

        checkCommonFields();
        Assertions.assertTrue(memoryAppender.contains(ch.qos.logback.classic.Level.INFO,CORRELATION_ID));

        Assertions.assertEquals(
                "CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=Reward dstip=%s msg=The refund has been calculated suser=%s cs1Label=TRXIssuer cs1=%s cs2Label=TRXAcquirer cs2=%s cs3Label=rewards cs3=%s cs4Label=correlationId cs4=%s"
                        .formatted(
                                AuditUtilities.SRCIP,
                                USER,
                                TRX_ISSUER,
                                TRX_ACQUIRER,
                                REWARD,
                                CORRELATION_ID
                                ),
                memoryAppender.getLoggedEvents().get(0).getMessage()
        );
    }


    public static class MemoryAppender extends ListAppender<ILoggingEvent> {
        public void reset() {
            this.list.clear();
        }

        public boolean contains(ch.qos.logback.classic.Level level, String string) {
            return this.list.stream()
                    .anyMatch(event -> event.toString().contains(string)
                            && event.getLevel().equals(level));
        }

        public int getSize() {
            return this.list.size();
        }

        public List<ILoggingEvent> getLoggedEvents() {
            return Collections.unmodifiableList(this.list);
        }
    }

}
