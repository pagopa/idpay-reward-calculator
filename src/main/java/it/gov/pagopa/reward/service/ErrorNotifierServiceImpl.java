package it.gov.pagopa.reward.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.KafkaException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Hooks;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {

    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";

    private final StreamBridge streamBridge;

    private final String rewardRuleBuilderMessagingServiceType;
    private final String rewardRuleBuilderServer;
    private final String rewardRuleBuilderTopic;

    private final String trxMessagingServiceType;
    private final String trxServer;
    private final String trxTopic;

    private final String hpanUpdateMessagingServiceType;
    private final String hpanUpdateServer;
    private final String hpanUpdateTopic;

    public ErrorNotifierServiceImpl(StreamBridge streamBridge,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-splitter.type}") String rewardRuleBuilderMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay-splitter.environment.spring.cloud.stream.kafka.binder.brokers}") String rewardRuleBuilderServer,
                                    @Value("${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.destination}") String rewardRuleBuilderTopic,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-rule.type}") String trxMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay-rule.environment.spring.cloud.stream.kafka.binder.brokers}") String trxServer,
                                    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.destination}") String trxTopic,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-hpan-update.type}") String hpanUpdateMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay-hpan-update.environment.spring.cloud.stream.kafka.binder.brokers}") String hpanUpdateServer,
                                    @Value("${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.destination}") String hpanUpdateTopic) {
        this.streamBridge = streamBridge;

        this.rewardRuleBuilderMessagingServiceType = rewardRuleBuilderMessagingServiceType;
        this.rewardRuleBuilderServer = rewardRuleBuilderServer;
        this.rewardRuleBuilderTopic = rewardRuleBuilderTopic;

        this.trxMessagingServiceType = trxMessagingServiceType;
        this.trxServer = trxServer;
        this.trxTopic = trxTopic;

        this.hpanUpdateMessagingServiceType = hpanUpdateMessagingServiceType;
        this.hpanUpdateServer = hpanUpdateServer;
        this.hpanUpdateTopic = hpanUpdateTopic;

        Hooks.onNextError((e, data) -> {
            if ((e instanceof MessageHandlingException messageHandlingException) && (messageHandlingException.getMostSpecificCause() instanceof KafkaException || messageHandlingException.getMostSpecificCause() instanceof TimeoutException)) {
                if (e.getMessage().contains(trxTopic) && data instanceof Message<?> message) {
                    log.error("[UNEXPECTED_TRX_PROCESSOR_ERROR] Unexpected error occurred evaluating transaction", e);
                    notifyTransactionEvaluation(message, "Unexpected error occurred", true, e);
                    return null;
                }
            }

            return e;
        });
    }

    @Override
    public void notifyRewardRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(rewardRuleBuilderMessagingServiceType, rewardRuleBuilderServer, rewardRuleBuilderTopic, message, description, retryable, exception);
    }

    @Override
    public void notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(trxMessagingServiceType, trxServer, trxTopic, message, description, retryable, exception);
    }

    @Override
    public void notifyHpanUpdateEvaluation(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(hpanUpdateMessagingServiceType, hpanUpdateServer, hpanUpdateTopic, message, description, retryable, exception);
    }

    @Override
    public void notify(String srcType, String srcServer, String srcTopic, Message<?> message, String description, boolean retryable, Throwable exception) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", description, exception);
        final MessageBuilder<?> errorMessage = MessageBuilder.fromMessage(message)
                .setHeader(ERROR_MSG_HEADER_SRC_TYPE, srcType)
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
