package it.gov.pagopa.common.stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.*;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {StreamsHealthIndicator.class, HealthEndpointAutoConfiguration.class})
class StreamsHealthIndicatorTest {

    @Autowired
    private HealthEndpoint healthEndpoint;
    @Autowired
    private StreamsHealthIndicator indicator;

    @Test
    void test() {
        performHealthCheck(Status.UP);

        indicator.afterSendCompletion(MessageBuilder.withPayload("MESSAGE").build(), Mockito.mock(MessageChannel.class), false, new RuntimeException());
        performHealthCheck(Status.UP);

        indicator.afterSendCompletion(MessageBuilder.withPayload("MESSAGE").build(), Mockito.mock(MessageChannel.class), false, new IllegalStateException(""));
        performHealthCheck(Status.UP);

        indicator.afterSendCompletion(MessageBuilder.withPayload("MESSAGE").build(), Mockito.mock(MessageChannel.class), false, new IllegalStateException("The [bean 'dummy_channel'] doesn't have subscribers to accept messages"));
        HealthComponent downHealth = performHealthCheck(Status.DOWN);
        Assertions.assertEquals(
                0,
                ((Health)((SystemHealth) downHealth).getComponents().get("streams")).getDetails().get("dummy_channel")
        );
    }

    private HealthComponent performHealthCheck(Status expectedStatus) {
        HealthComponent health = healthEndpoint.health();
        Assertions.assertEquals(expectedStatus, health.getStatus());
        HealthComponent streamsHealthComponents = ((SystemHealth) health).getComponents().get("streams");
        Assertions.assertNotNull(streamsHealthComponents);
        Assertions.assertEquals(expectedStatus, streamsHealthComponents.getStatus());
        return health;
    }
}
