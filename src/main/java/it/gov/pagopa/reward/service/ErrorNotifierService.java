package it.gov.pagopa.reward.service;

import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    void notifyRewardRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyHpanUpdateEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    void notify(String srcType, String srcServer, String srcTopic, Message<?> message, String description, boolean retryable, Throwable exception);
}
