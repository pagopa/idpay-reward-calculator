package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.service.synchronous.op.CancelTrxSynchronousService;
import it.gov.pagopa.reward.service.synchronous.op.CreateTrxSynchronousService;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionAuthRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionResponseDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RewardTrxSynchronousApiServiceTest {

    @Mock
    private CreateTrxSynchronousService createTrxSynchronousServiceMock;
    @Mock
    private CancelTrxSynchronousService cancelTrxSynchronousServiceMock;

    private RewardTrxSynchronousApiService rewardTrxSynchronousApiService;

    @BeforeEach
    void setUp() {
        rewardTrxSynchronousApiService = new RewardTrxSynchronousApiServiceImpl(createTrxSynchronousServiceMock, cancelTrxSynchronousServiceMock);
    }

    @Test
    void previewTransaction() {
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTOFaker.mockInstance(1);

        Mockito.when(createTrxSynchronousServiceMock.previewTransaction(request, "INITIATIVEID"))
                        .thenReturn(Mono.just(response));

        SynchronousTransactionResponseDTO result = rewardTrxSynchronousApiService.previewTransaction(request, "INITIATIVEID").block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(response, result);
        Mockito.verify(createTrxSynchronousServiceMock, Mockito.only()).previewTransaction(Mockito.any(),Mockito.any());
    }

    @Test
    void authorizeTransaction() {
        SynchronousTransactionAuthRequestDTO request = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTOFaker.mockInstance(1);

        Mockito.when(createTrxSynchronousServiceMock.authorizeTransaction(request, "INITIATIVEID", 1))
                        .thenReturn(Mono.just(response));

        SynchronousTransactionResponseDTO result = rewardTrxSynchronousApiService.authorizeTransaction(request, "INITIATIVEID", 1).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(response, result);
        Mockito.verify(createTrxSynchronousServiceMock, Mockito.only()).authorizeTransaction(Mockito.any(), Mockito.any(), Mockito.anyLong());
    }

    @Test
    void cancelTransaction() {
        SynchronousTransactionAuthRequestDTO request = SynchronousTransactionAuthRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTOFaker.mockInstance(1);

        Mockito.when(cancelTrxSynchronousServiceMock.cancelTransaction(request, "INITIATIVEID"))
                .thenReturn(Mono.just(response));

        SynchronousTransactionResponseDTO result = rewardTrxSynchronousApiService.cancelTransaction(request, "INITIATIVEID").block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(response, result);
        Mockito.verify(cancelTrxSynchronousServiceMock, Mockito.only()).cancelTransaction(Mockito.any(), Mockito.any());
    }
}