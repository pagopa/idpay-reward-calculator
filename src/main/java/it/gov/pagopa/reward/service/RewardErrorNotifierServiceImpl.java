package it.gov.pagopa.reward.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RewardErrorNotifierServiceImpl implements RewardErrorNotifierService {

    private final ErrorNotifierService errorNotifierService;

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
    public RewardErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,

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
        this.errorNotifierService = errorNotifierService;

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
        return errorNotifierService.notify(srcType, srcServer, srcTopic, group, message, description, retryable,resendApplication, exception);
    }
}