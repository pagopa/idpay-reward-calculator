package it.gov.pagopa.common.stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;

class StreamsHealthIndicatorTest {

    @Test
    void test() {
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Mockito.when(mockContext.getBeansOfType(DirectWithAttributesChannel.class))
                .thenReturn(Collections.emptyMap());

        StreamsHealthIndicator indicator = new StreamsHealthIndicator(mockContext);

        Assertions.assertEquals("UP", indicator.health().getStatus().getCode());

        indicator.afterSendCompletion(
                MessageBuilder.withPayload("MESSAGE").build(),
                Mockito.mock(MessageChannel.class),
                false,
                new IllegalStateException("The [bean 'dummy_channel'] doesn't have subscribers to accept messages")
        );
        Assertions.assertEquals("DOWN", indicator.health().getStatus().getCode());
    }


}
