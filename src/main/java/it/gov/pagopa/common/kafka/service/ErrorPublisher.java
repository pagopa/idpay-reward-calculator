package it.gov.pagopa.common.kafka.service;

import org.springframework.messaging.Message;

public interface ErrorPublisher {
    boolean send(Message<?> message);
}
