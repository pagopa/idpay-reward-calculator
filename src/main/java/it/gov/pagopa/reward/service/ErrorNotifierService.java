package it.gov.pagopa.reward.service;

import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    void notifyRewardRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRewardedTransaction(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyHpanUpdateEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable, Throwable exception);
}