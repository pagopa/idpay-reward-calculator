package it.gov.pagopa.common.reactive.stream.service;

import it.gov.pagopa.common.kafka.service.ErrorPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
@Slf4j
public class ErrorPublisherImpl implements ErrorPublisher {

    private final StreamBridge streamBridge;

    public ErrorPublisherImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean send(Message<?> errorMessage) {
        return streamBridge.send("errors-out-0", errorMessage);
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class ErrorNotifierProducerConfig {
        @Bean
        public Supplier<Flux<Message<Object>>> errors() {
            return Flux::empty;
        }
    }
}
