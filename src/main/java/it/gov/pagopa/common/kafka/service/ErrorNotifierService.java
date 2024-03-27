package it.gov.pagopa.common.kafka.service;


import it.gov.pagopa.reward.config.KafkaConfiguration;
import org.springframework.messaging.Message;

public interface ErrorNotifierService {

    boolean notify(KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO, String  description, boolean retryable, Throwable exception, boolean resendApplication, Message<?> message);
}