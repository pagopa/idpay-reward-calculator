package it.gov.pagopa.common.kafka.service;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {

    private final ErrorPublisher errorPublisher;
    private final String applicationName;

    public ErrorNotifierServiceImpl(
            @Value("${spring.application.name}") String applicationName,

            ErrorPublisher errorPublisher) {
        this.errorPublisher = errorPublisher;
        this.applicationName = applicationName;
    }

    @Override
    public boolean notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable,boolean resendApplication, Throwable exception) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", description, exception);
        final MessageBuilder<?> errorMessage = MessageBuilder.fromMessage(message)
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_SRC_TYPE, srcType)
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_SRC_SERVER, srcServer)
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_SRC_TOPIC, srcTopic)
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_DESCRIPTION, description)
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_RETRYABLE, retryable)
                .setHeader(KafkaConstants.ERROR_MSG_HEADER_STACKTRACE, ExceptionUtils.getStackTrace(exception));

        addExceptionInfo(errorMessage, "rootCause", ExceptionUtils.getRootCause(exception));
        addExceptionInfo(errorMessage, "cause", exception.getCause());

        byte[] receivedKey = message.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY, byte[].class);
        if(receivedKey!=null){
            errorMessage.setHeader(KafkaHeaders.MESSAGE_KEY, new String(receivedKey, StandardCharsets.UTF_8));
        }

        if (resendApplication){
            errorMessage.setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, applicationName);
            errorMessage.setHeader(KafkaConstants.ERROR_MSG_HEADER_GROUP, group);
        }

        if (!errorPublisher.send(errorMessage.build())) {
            log.error("[ERROR_NOTIFIER] Something gone wrong while notifying error");
            return false;
        } else {
            return true;
        }
    }

    private void addExceptionInfo(MessageBuilder<?> errorMessage, String exceptionHeaderPrefix, Throwable rootCause) {
        errorMessage
                .setHeader("%sClass".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getClass().getName() : null)
                .setHeader("%sMessage".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getMessage() : null);
    }
}
