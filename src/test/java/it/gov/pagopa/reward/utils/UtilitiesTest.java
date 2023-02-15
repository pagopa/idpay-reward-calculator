package it.gov.pagopa.reward.utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = {Utilities.class,InetAddress.class})
class UtilitiesTest {
    private static final String SRCIP;

    static {
        try {
            SRCIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String CEF = String.format("CEF:0 srcip=%s ", SRCIP);
    private static final String MSG = " TEST_MSG";
    private static final String USER = "TEST_USER_ID";
    private static final String CORRELATION_ID = "TEST_CORRELATION_ID";
    private static final String TRX_ISSUER = "TEST_TRX_ISSUER";
    private static final String TRX_ACQUIRER = "TEST_TRX_ACQUIRER";
    private static final String REWARD = "TEST_REWARD";

    @MockBean
    Logger logger;
    @Autowired
    Utilities utilities;
    @MockBean
    InetAddress inetAddress;
    MemoryAppender memoryAppender;

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
        utilities.logCharge(USER, TRX_ISSUER, TRX_ACQUIRER, REWARD);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
    }

    @Test
    void logRefund_ok(){
        utilities.logRefund(USER, TRX_ISSUER, TRX_ACQUIRER, REWARD, CORRELATION_ID);
        assertThat(memoryAppender.contains(ch.qos.logback.classic.Level.DEBUG,MSG)).isFalse();
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

        public int countEventsForLogger(String loggerName) {
            return (int) this.list.stream()
                    .filter(event -> event.getLoggerName().contains(loggerName))
                    .count();
        }

        public List<ILoggingEvent> search() {
            return this.list.stream()
                    .filter(event -> event.toString().contains(MSG))
                    .collect(Collectors.toList());
        }

        public List<ILoggingEvent> search(Level level) {
            return this.list.stream()
                    .filter(event -> event.toString().contains(MSG)
                            && event.getLevel().equals(level))
                    .collect(Collectors.toList());
        }

        public int getSize() {
            return this.list.size();
        }

        public List<ILoggingEvent> getLoggedEvents() {
            return Collections.unmodifiableList(this.list);
        }
    }

}
