package it.gov.pagopa.reward.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {
    public static final String ERROR_MSG_HEADER_APPLICATION_NAME = "applicationName";
    public static final String ERROR_MSG_HEADER_GROUP = "group";

    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";

    private final StreamBridge streamBridge;
    private final String applicationName;

    private final String rewardRuleBuilderMessagingServiceType;
    private final String rewardRuleBuilderServer;
    private final String rewardRuleBuilderTopic;
    private final String rewardRuleBuilderGroup;

    private final String trxMessagingServiceType;
    private final String trxServer;
    private final String trxTopic;
    private final String trxGroup;

    private final String trxRewardedMessagingServiceType;
    private final String trxRewardedServer;
    private final String trxRewardedTopic;

    private final String hpanUpdateMessagingServiceType;
    private final String hpanUpdateServer;
    private final String hpanUpdateTopic;
    private final String hpanUpdateGroup;

    private final String hpanUpdateOutcomeMessagingServiceType;
    private final String hpanUpdateOutcomeServer;
    private final String hpanUpdateOutcomeTopic;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public ErrorNotifierServiceImpl(StreamBridge streamBridge,
                                    @Value("${spring.application.name}") String applicationName,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-splitter.type}") String rewardRuleBuilderMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay-splitter.environment.spring.cloud.stream.kafka.binder.brokers}") String rewardRuleBuilderServer,
                                    @Value("${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.destination}") String rewardRuleBuilderTopic,
                                    @Value("${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.group}") String rewardRuleBuilderGroup,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-rule.type}") String trxMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay-rule.environment.spring.cloud.stream.kafka.binder.brokers}") String trxServer,
                                    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.destination}") String trxTopic,
                                    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.group}") String trxGroup,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay.type}") String trxRewardedMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay.environment.spring.cloud.stream.kafka.binder.brokers}") String trxRewardedServer,
                                    @Value("${spring.cloud.stream.bindings.trxProcessorOut-out-0.destination}") String trxRewardedTopic,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay-hpan-update.type}") String hpanUpdateMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay-hpan-update.environment.spring.cloud.stream.kafka.binder.brokers}") String hpanUpdateServer,
                                    @Value("${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.destination}") String hpanUpdateTopic,
                                    @Value("${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.group}") String hpanUpdateGroup,

                                    @Value("${spring.cloud.stream.binders.kafka-hpan-update-outcome.type}") String hpanUpdateOutcomeMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-hpan-update-outcome.environment.spring.cloud.stream.kafka.binder.brokers}") String hpanUpdateOutcomeServer,
                                    @Value("${spring.cloud.stream.bindings.hpanUpdateOutcome-out-0.destination}") String hpanUpdateOutcomeTopic) {
        this.streamBridge = streamBridge;
        this.applicationName = applicationName;

        this.rewardRuleBuilderMessagingServiceType = rewardRuleBuilderMessagingServiceType;
        this.rewardRuleBuilderServer = rewardRuleBuilderServer;
        this.rewardRuleBuilderTopic = rewardRuleBuilderTopic;
        this.rewardRuleBuilderGroup = rewardRuleBuilderGroup;

        this.trxMessagingServiceType = trxMessagingServiceType;
        this.trxServer = trxServer;
        this.trxTopic = trxTopic;
        this.trxGroup = trxGroup;

        this.trxRewardedMessagingServiceType = trxRewardedMessagingServiceType;
        this.trxRewardedServer = trxRewardedServer;
        this.trxRewardedTopic = trxRewardedTopic;

        this.hpanUpdateMessagingServiceType = hpanUpdateMessagingServiceType;
        this.hpanUpdateServer = hpanUpdateServer;
        this.hpanUpdateTopic = hpanUpdateTopic;
        this.hpanUpdateGroup = hpanUpdateGroup;

        this.hpanUpdateOutcomeMessagingServiceType = hpanUpdateOutcomeMessagingServiceType;
        this.hpanUpdateOutcomeServer = hpanUpdateOutcomeServer;
        this.hpanUpdateOutcomeTopic = hpanUpdateOutcomeTopic;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class ErrorNotifierProducerConfig {
        @Bean
        public Supplier<Flux<Message<Object>>> errors() {
            return Flux::empty;
        }
    }

    @Override
    public boolean notifyRewardRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(rewardRuleBuilderMessagingServiceType, rewardRuleBuilderServer, rewardRuleBuilderTopic, rewardRuleBuilderGroup, message, description, retryable, true, exception);
    }

    @Override
    public boolean notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(trxMessagingServiceType, trxServer, trxTopic, trxGroup, message, description, retryable, true, exception);
    }

    @Override
    public boolean notifyRewardedTransaction(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(trxRewardedMessagingServiceType, trxRewardedServer, trxRewardedTopic,null, message, description, retryable, false, exception);
    }

    @Override
    public boolean notifyHpanUpdateEvaluation(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(hpanUpdateMessagingServiceType, hpanUpdateServer, hpanUpdateTopic, hpanUpdateGroup, message, description, retryable, true, exception);
    }

    @Override
    public boolean notifyHpanUpdateOutcome(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(hpanUpdateOutcomeMessagingServiceType, hpanUpdateOutcomeServer, hpanUpdateOutcomeTopic,null, message, description, retryable, false, exception);
    }

    @Override
    public boolean notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable,boolean resendApplication, Throwable exception) {
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

        byte[] receivedKey = message.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY, byte[].class);
        if(receivedKey!=null){
            errorMessage.setHeader(KafkaHeaders.MESSAGE_KEY, new String(receivedKey, StandardCharsets.UTF_8));
        }

        if (resendApplication){
            errorMessage.setHeader(ERROR_MSG_HEADER_APPLICATION_NAME, applicationName);
            errorMessage.setHeader(ERROR_MSG_HEADER_GROUP, group);
        }

        if (!streamBridge.send("errors-out-0", errorMessage.build())) {
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
