package it.gov.pagopa.reward.service.counters;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class UserInitiativeCountersUnlockMediatorServiceTest {

    @Mock
    UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;

    @Mock
    RewardErrorNotifierService rewardErrorNotifierServiceMock;

    UserInitiativeCountersUnlockMediatorService  userInitiativeCountersUnlockMediatorService;

    @BeforeEach
    void setUp() {
        userInitiativeCountersUnlockMediatorService = new UserInitiativeCountersUnlockMediatorServiceImpl(
                "appName",
                userInitiativeCountersRepositoryMock,
                rewardErrorNotifierServiceMock,
                0L,
                TestUtils.objectMapper);
    }

    @Test
    void execute_UnexpectedJson() {
        userInitiativeCountersUnlockMediatorService.execute(Flux
                .just(getMessage("INVALID JSON")));

        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.times(1)).notifyTransactionResponse(any(), any(), eq(true), any());
    }

    @Test
    void execute_AcceptedStateAuthorized() {
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);
        rewardTransactionDTO.setStatus(RewardConstants.PAYMENT_STATE_AUTHORIZED);

        Mockito.when(userInitiativeCountersRepositoryMock.unlockPendingTrx(rewardTransactionDTO.getId()))
                .thenReturn(Mono.just(new UserInitiativeCounters()));

        userInitiativeCountersUnlockMediatorService.execute(Flux.just(getMessage(TestUtils.jsonSerializer(rewardTransactionDTO))));

        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.times(1)).unlockPendingTrx(any());
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.times(0)).notifyTransactionResponse(any(), any(), eq(true), any());

    }

    @Test
    void execute_NotAcceptedState () {
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);
        rewardTransactionDTO.setStatus(RewardConstants.REWARD_STATE_REJECTED);

        userInitiativeCountersUnlockMediatorService.execute(Flux.just(getMessage(TestUtils.jsonSerializer(rewardTransactionDTO))));

        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.times(0)).unlockPendingTrx(any());
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.times(0)).notifyTransactionResponse(any(), any(), eq(true), any());

    }

    @Test
    void execute_WithException() {
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);
        rewardTransactionDTO.setStatus(RewardConstants.PAYMENT_STATE_AUTHORIZED);

        Mockito.when(userInitiativeCountersRepositoryMock.unlockPendingTrx(rewardTransactionDTO.getId()))
                .thenThrow(new RuntimeException("Dummy exception"));

        userInitiativeCountersUnlockMediatorService.execute(Flux.just(getMessage(TestUtils.jsonSerializer(rewardTransactionDTO))));

        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.times(1)).unlockPendingTrx(any());
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.times(1)).notifyTransactionResponse(any(), any(), eq(true), any());

    }

    @NotNull
    private static Message<String> getMessage(String jsonMessage) {
        return MessageBuilder.withPayload(jsonMessage)
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, Mockito.mock(Acknowledgment.class))
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                .setHeader(KafkaHeaders.OFFSET, 0L)
                .build();
    }
}