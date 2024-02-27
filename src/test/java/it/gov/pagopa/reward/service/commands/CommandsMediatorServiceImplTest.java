package it.gov.pagopa.reward.service.commands;

import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.dto.commands.CommandOperationDTO;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.commands.ops.DeleteInitiativeService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.CommandsConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.KieBase;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class CommandsMediatorServiceImplTest {

    @Mock
    private DeleteInitiativeService deleteInitiativeServiceMock;
    @Mock
    private RewardErrorNotifierService rewardErrorNotifierServiceMock;
    @Mock
    private RewardContextHolderService rewardContextHolderServiceMock;

    private CommandsMediatorServiceImpl commandsMediatorService;

    @BeforeEach
    void setUp() {
        commandsMediatorService = new CommandsMediatorServiceImpl("appName", 100L, "PT1S", deleteInitiativeServiceMock, rewardErrorNotifierServiceMock, rewardContextHolderServiceMock, TestUtils.objectMapper);
    }

    @Test
    void getCommitDelay() {
        Duration result = commandsMediatorService.getCommitDelay();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(Duration.ofMillis(100L), result);
    }

    @Test
    void subscribeAfterCommits() {
        Flux<List<String>> afterCommits2subscribe = Flux.just(List.of("INITIATIVE1", "INITIATIVE2"));

        Mockito.when(rewardContextHolderServiceMock.refreshKieContainerCacheMiss())
                .thenReturn(Mono.just(Mockito.mock(KieBase.class)));

        commandsMediatorService.subscribeAfterCommits(afterCommits2subscribe);

        Mockito.verify(rewardContextHolderServiceMock, Mockito.only()).refreshKieContainerCacheMiss();
    }

    @Test
    void getObjectReader() {
        ObjectReader result = commandsMediatorService.getObjectReader();

        Assertions.assertNotNull(result);
    }

    @Test
    void execute_operationDelete() {
        CommandOperationDTO payload = CommandOperationDTO.builder()
                .entityId("DUMMY_INITITATIVEID")
                .operationTime(LocalDateTime.now())
                .operationType(CommandsConstants.COMMANDS_OPERATION_TYPE_DELETE_INITIATIVE)
                .build();

        Message<String> message = MessageBuilder.withPayload("DUMMY_INITITATIVEID").setHeader("HEADER","DUMMY_HEADER").build();
        Map<String, Object> ctx = new HashMap<>();

        Mockito.when(deleteInitiativeServiceMock.execute(payload.getEntityId())).thenReturn(Mono.just("DUMMY_INITITATIVEID"));

        String result = commandsMediatorService.execute(payload, message, ctx).block();

        //then
        Assertions.assertNotNull(result);
        Mockito.verify(deleteInitiativeServiceMock).execute(anyString());
    }

    @Test
    void execute_anotherOperation() {
        CommandOperationDTO payload = CommandOperationDTO.builder()
                .entityId("DUMMY_INITITATIVEID")
                .operationTime(LocalDateTime.now())
                .operationType("OTHER_OPERATION_TYPE")
                .build();

        Message<String> message = MessageBuilder.withPayload("DUMMY_INITITATIVEID").setHeader("HEADER","DUMMY_HEADER").build();
        Map<String, Object> ctx = new HashMap<>();

        String result = commandsMediatorService.execute(payload, message, ctx).block();

        //then
        Assertions.assertNull(result);
        Mockito.verify(deleteInitiativeServiceMock, Mockito.never()).execute(anyString());
    }

    @Test
    void getFlowName() {
        String result = commandsMediatorService.getFlowName();

        Assertions.assertEquals("REWARD_CALCULATOR_COMMANDS", result);
    }

    @Test
    void getConcurrency() {
        int result = commandsMediatorService.getConcurrency();

        Assertions.assertEquals(1, result);
    }

    @Test
    void onDeserializationError(){
        Message<String> dummyMessage = MessageBuilder.withPayload("DUMMY_MESSAGE").build();

        Consumer<Throwable> result = commandsMediatorService.onDeserializationError(dummyMessage);

        Assertions.assertNotNull(result);
    }

    @Test
    void notifyError(){
        Message<String> dummyMessage = MessageBuilder.withPayload("DUMMY_MESSAGE").build();
        RuntimeException exception = new RuntimeException("DUMMY_EXCEPTION");


        Mockito.doNothing().when(rewardErrorNotifierServiceMock)
                .notifyRewardCommands(dummyMessage, "[REWARD_CALCULATOR_COMMANDS] An error occurred evaluating commands", true, exception);


        commandsMediatorService.notifyError(dummyMessage, exception);

        Mockito.verifyNoMoreInteractions(rewardErrorNotifierServiceMock);
    }
}