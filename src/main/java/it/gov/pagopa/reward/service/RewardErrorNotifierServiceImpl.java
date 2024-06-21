package it.gov.pagopa.reward.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.reward.config.KafkaConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RewardErrorNotifierServiceImpl implements RewardErrorNotifierService {

    private final ErrorNotifierService errorNotifierService;
    private final KafkaConfiguration kafkaConfiguration;

    private static final String REWARD_RULE_CONSOMER_IN_0 = "rewardRuleConsumer-in-0";
    private static final  String TRX_PROCESSOR_IN_0 = "trxProcessor-in-0";
    private static final  String TRX_PROCESSOR_OUT_0 = "trxProcessorOut-out-0";
    private static final String HPAN_INITIATIVE_CONSUMER_IN_0 = "hpanInitiativeConsumer-in-0";
    private static final String HPAN_UPDATE_OUTCOME_OUT_0 = "hpanUpdateOutcome-out-0";
    private static final String COMMANDS_CONSUMER_IN_0 = "commandsConsumer-in-0";
    private static final String TRX_RESPONSE_CONSUMER_IN_0 = "trxResponseConsumer-in-0";

    public RewardErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,
                                          KafkaConfiguration kafkaConfiguration
    ) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.errorNotifierService = errorNotifierService;
    }

    @Override
    public boolean notifyRewardRuleBuilder(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(REWARD_RULE_CONSOMER_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public boolean notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(TRX_PROCESSOR_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public boolean notifyRewardedTransaction(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(TRX_PROCESSOR_OUT_0),message, description, retryable, false, exception);
    }

    @Override
    public boolean notifyHpanUpdateEvaluation(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(HPAN_INITIATIVE_CONSUMER_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public boolean notifyHpanUpdateOutcome(Message<?> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(HPAN_UPDATE_OUTCOME_OUT_0), message, description, retryable, false, exception);
    }

    @Override
    public void notifyRewardCommands(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(COMMANDS_CONSUMER_IN_0), message, description, retryable, true, exception);
    }

    @Override
    public boolean notifyTransactionResponse(Message<String> message, String description, boolean retryable, Throwable exception) {
        return notify(kafkaConfiguration.getStream().getBindings().get(TRX_RESPONSE_CONSUMER_IN_0), message, description, retryable, true, exception);

    }

    @Override
    public boolean notify(KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        return errorNotifierService.notify(baseKafkaInfoDTO, description, retryable,exception, resendApplication, message);
    }
}
