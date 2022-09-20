package it.gov.pagopa.reward.event.processor;

import it.gov.pagopa.reward.service.reward.RewardCalculatorMediatorService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.BindingCreatedEvent;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class TransactionProcessor implements ApplicationListener<RewardContextHolderServiceImpl.RewardContextHolderReadyEvent> {

    public static final String TRX_PROCESSOR_BINDING_NAME = "trxProcessor-in-0";

    private final BindingsLifecycleController bindingsLifecycleController;
    private final RewardCalculatorMediatorService rewardCalculatorMediatorService;

    private boolean contextReady = false;

    public TransactionProcessor(BindingsLifecycleController bindingsLifecycleController, RewardCalculatorMediatorService rewardCalculatorMediatorService) {
        this.bindingsLifecycleController = bindingsLifecycleController;
        this.rewardCalculatorMediatorService = rewardCalculatorMediatorService;
    }

    /**
     * Read from the topic ${KAFKA_TOPIC_RTD_TRX} and publish to topic ${KAFKA_TOPIC_REWARD_TRX}
     */
    @Bean
    public Consumer<Flux<Message<String>>> trxProcessor() {
        return rewardCalculatorMediatorService::execute;
    }

    @EventListener(BindingCreatedEvent.class)
    public void onBindingCreatedEvent(BindingCreatedEvent event) {
        if (event.getSource() instanceof Binding<?> binding && TRX_PROCESSOR_BINDING_NAME.equals(binding.getBindingName()) && contextReady) {
            synchronized (this) {
                binding.start();
            }
        }
    }

    @Override
    public void onApplicationEvent(RewardContextHolderServiceImpl.RewardContextHolderReadyEvent event) {
        synchronized (this) {
            contextReady = true;
            bindingsLifecycleController.changeState(TRX_PROCESSOR_BINDING_NAME, BindingsLifecycleController.State.STARTED);
        }
    }
}
