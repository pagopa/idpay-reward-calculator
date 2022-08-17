package it.gov.pagopa.reward.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {

    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";

    private final StreamBridge streamBridge;

    private final String rewardRuleBuilderServer;
    private final String rewardRuleBuilderTopic;

    private final String trxServer;
    private final String trxTopic;

    public ErrorNotifierServiceImpl(StreamBridge streamBridge,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-splitter.environment.spring.cloud.stream.kafka.binder.brokers}") String rewardRuleBuilderServer,
                                    @Value("${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.destination}") String rewardRuleBuilderTopic,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-rule.environment.spring.cloud.stream.kafka.binder.brokers}") String trxServer,
                                    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.destination}") String trxTopic) {
        this.streamBridge = streamBridge;

        this.rewardRuleBuilderServer = rewardRuleBuilderServer;
        this.rewardRuleBuilderTopic = rewardRuleBuilderTopic;

        this.trxServer = trxServer;
        this.trxTopic = trxTopic;
    }

    @Override
    public void notifyRewardRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(rewardRuleBuilderServer, rewardRuleBuilderTopic, message, description, retryable, exception);
    }

    @Override
    public void notifyTransaction(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(trxServer, trxTopic, message, description, retryable, exception);
    }

    @Override
    public void notify(String srcServer, String srcTopic, Message<?> message, String description, boolean retryable, Throwable exception) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", description, exception);
        final MessageBuilder<?> errorMessage = MessageBuilder.fromMessage(message)
                .setHeader(ERROR_MSG_HEADER_SRC_SERVER, srcServer)
                .setHeader(ERROR_MSG_HEADER_SRC_TOPIC, srcTopic)
                .setHeader(ERROR_MSG_HEADER_DESCRIPTION, description)
                .setHeader(ERROR_MSG_HEADER_RETRYABLE, retryable)
                .setHeader(ERROR_MSG_HEADER_STACKTRACE, ExceptionUtils.getStackTrace(exception));

        addExceptionInfo(errorMessage, "rootCause", ExceptionUtils.getRootCause(exception));
        addExceptionInfo(errorMessage, "cause", exception.getCause());

        if (!streamBridge.send("errors-out-0", errorMessage.build())) {
            log.error("[ERROR_NOTIFIER] Something gone wrong while notifying error");
        }
    }

    private void addExceptionInfo(MessageBuilder<?> errorMessage, String exceptionHeaderPrefix, Throwable rootCause) {
        errorMessage
                .setHeader("%sClass".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getClass().getName() : null)
                .setHeader("%sMessage".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getMessage() : null);
    }
}
