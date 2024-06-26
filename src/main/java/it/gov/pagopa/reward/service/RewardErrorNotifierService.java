package it.gov.pagopa.reward.service;

import it.gov.pagopa.reward.config.KafkaConfiguration;
import org.springframework.messaging.Message;

public interface RewardErrorNotifierService {
    boolean notifyRewardRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyRewardedTransaction(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyHpanUpdateEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    boolean notifyHpanUpdateOutcome(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyRewardCommands(Message<String> message, String description, boolean retryable, Throwable exception);
    boolean notifyTransactionResponse(Message<String> message, String description, boolean retryable, Throwable exception);
    boolean notify(KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}