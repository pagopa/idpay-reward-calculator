package it.gov.pagopa.common.kafka.service;

import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    boolean notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}