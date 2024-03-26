package it.gov.pagopa.common.kafka.service;

import it.gov.pagopa.reward.model.SrcDetails;
import org.springframework.messaging.Message;

public interface ErrorNotifierService {

    boolean notify(SrcDetails srcDetails, String  description, boolean retryable, Throwable exception, String group, boolean resendApplication, Message<?> message);
}