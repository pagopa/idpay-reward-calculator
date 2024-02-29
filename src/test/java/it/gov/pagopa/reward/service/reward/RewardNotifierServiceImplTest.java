package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.common.reactive.kafka.exception.UncommittableError;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RewardNotifierServiceImplTest {
    @Mock
    private StreamBridge streamBridgeMock;

    @Mock
    private RewardErrorNotifierService rewardErrorNotifierServiceMock;

    private static final int rewardNotifyMaxRetries = 3;
    private RewardNotifierService rewardNotifierService;

    @BeforeEach
    void setUp() {
        rewardNotifierService = new RewardNotifierServiceImpl(rewardNotifyMaxRetries, streamBridgeMock, rewardErrorNotifierServiceMock);

    }

    @Test
    void rewardNotifierPublisherConfigNotNull(){
        RewardNotifierServiceImpl.RewardNotifierPublisherConfig rewardNotifierPublisherConfig = new RewardNotifierServiceImpl.RewardNotifierPublisherConfig();

        Supplier<Flux<Message<RewardTransactionDTO>>> result = rewardNotifierPublisherConfig.trxProcessorOut();

        Assertions.assertNotNull(result);
    }

    @Test
    void testNotify() {
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);

        Mockito.when(streamBridgeMock.send(Mockito.eq("trxProcessorOut-out-0"), Mockito.any()))
                .thenReturn(true);

        boolean result = rewardNotifierService.notify(rewardTransactionDTO);

        Assertions.assertTrue(result);
    }

    @Test
    void notifyFallbackToErrorTopic() {
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);

        Mockito.when(streamBridgeMock.send(Mockito.eq("trxProcessorOut-out-0"), Mockito.any()))
                .thenReturn(false);

        Mockito.when(rewardErrorNotifierServiceMock.notifyRewardedTransaction(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(true);

        rewardNotifierService.notifyFallbackToErrorTopic(rewardTransactionDTO);

        Mockito.verify(streamBridgeMock, Mockito.only()).send(Mockito.any(), Mockito.any());

    }

    @Test
    void notifyFallbackToErrorTopicError() {
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);

        Mockito.when(streamBridgeMock.send(Mockito.eq("trxProcessorOut-out-0"), Mockito.any()))
                .thenReturn(false);

        Mockito.when(rewardErrorNotifierServiceMock.notifyRewardedTransaction(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
                .thenReturn(false);

        UncommittableError exceptionResult = assertThrows(UncommittableError.class, () -> rewardNotifierService.notifyFallbackToErrorTopic(rewardTransactionDTO));

        Assertions.assertEquals("[UNEXPECTED_TRX_PROCESSOR_ERROR] Cannot publish result neither in error topic!", exceptionResult.getMessage());
        Mockito.verify(streamBridgeMock, Mockito.times(rewardNotifyMaxRetries+1)).send(Mockito.any(), Mockito.any());

    }

}