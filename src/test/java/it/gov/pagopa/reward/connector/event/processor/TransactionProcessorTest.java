package it.gov.pagopa.reward.connector.event.processor;

import it.gov.pagopa.reward.service.reward.RewardCalculatorMediatorService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.BindingCreatedEvent;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {
    @Mock
    private BindingsLifecycleController bindingsLifecycleControllerMock;
    @Mock
    private RewardCalculatorMediatorService rewardCalculatorMediatorServiceMock;
    private TransactionProcessor transactionProcessor;

    @BeforeEach
    void setUp() {
        transactionProcessor = new TransactionProcessor(bindingsLifecycleControllerMock, rewardCalculatorMediatorServiceMock);
    }

    @Test
    void trxProcessor() {
        Consumer<Flux<Message<String>>> result = transactionProcessor.trxProcessor();

        Assertions.assertNotNull(result);
    }

    @Test
    void onBindingCreatedEvent() {
        Binding binding = Mockito.mock(Binding.class);
        Mockito.when(binding.getBindingName()).thenReturn("trxProcessor-in-0");
        BindingCreatedEvent event = new BindingCreatedEvent(binding);

        transactionProcessor.onBindingCreatedEvent(event);

        Mockito.verify(binding, Mockito.only()).getBindingName();
    }

    @Test
    void onApplicationEvent() {

        Mockito.doNothing()
                .when(bindingsLifecycleControllerMock).changeState("trxProcessor-in-0", BindingsLifecycleController.State.STARTED);
        RewardContextHolderServiceImpl.RewardContextHolderReadyEvent event = new RewardContextHolderServiceImpl.RewardContextHolderReadyEvent(Mockito.mock(Object.class));

        transactionProcessor.onApplicationEvent(event);

        Mockito.verify(bindingsLifecycleControllerMock, Mockito.only()).changeState(Mockito.any(), Mockito.any());
    }
}