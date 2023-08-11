package it.gov.pagopa.common.config;

import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.utils.MemoryAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {HealthIndicatorLogger.class, HealthEndpointAutoConfiguration.class})
class HealthIndicatorLoggerTest {

    @MockBean
    private DiskSpaceHealthIndicator diskSpaceHealthIndicatorMock;
    private MemoryAppender memoryAppender;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(HealthIndicatorLogger.class.getName());
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();

        Mockito.when(diskSpaceHealthIndicatorMock.health())
                .thenReturn(Health.down().withDetails(Map.of("DETAILKEY", "DETAILVALUE")).build());
    }
    @Test
    void testDown(){
        test(Status.DOWN);
    }

    @Test
    void testOutOfService(){
        Mockito.when(diskSpaceHealthIndicatorMock.health())
                .thenReturn(Health.outOfService().withDetails(Map.of("DETAILKEY", "DETAILVALUE")).build());

        test(Status.OUT_OF_SERVICE);
    }

    private void test(Status expectedStatus) {
        healthEndpoint.health();

        Assertions.assertEquals(1, memoryAppender.getLoggedEvents().size());
        Assertions.assertEquals("[HEALTH][%s] DiskSpaceHealthIndicator: {DETAILKEY=DETAILVALUE}".formatted(expectedStatus),
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage());
    }
}
