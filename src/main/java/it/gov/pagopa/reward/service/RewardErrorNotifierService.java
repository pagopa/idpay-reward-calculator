package it.gov.pagopa.reward.service;

import org.springframework.messaging.Message;

public interface RewardErrorNotifierService {
    boolean notifyRewardRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyRewardedTransaction(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyHpanUpdateEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyHpanUpdateOutcome(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRewardCommands(Message<String> message, String description, boolean retryable, Throwable exception);
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    boolean notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}