package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.exception.TransactionSynchronousException;
import it.gov.pagopa.reward.service.synchronous.RewardTrxSynchronousApiService;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.List;

@WebFluxTest(controllers = {RewardTrxSynchronousApiController.class})
class RewardTrxSynchronousApiControllerImplTest {
    private final String rewardPreviewPath = "/reward/preview/{initiativeId}";

    @MockBean
    RewardTrxSynchronousApiService rewardTrxSynchronousServiceMock;

    @Autowired
    protected WebTestClient webClient;

    @Test
    void postTransactionPreviewOK(){

        String initiativeId = "INITIATIVEID";
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTO.builder()
                .initiativeId(initiativeId)
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .build();

        Mockito.when(rewardTrxSynchronousServiceMock.previewTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenReturn(Mono.just(response));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardPreviewPath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(response);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).previewTransaction(Mockito.any(), Mockito.eq(initiativeId));
    }

    @Test
    void postTransactionPreviewUserNotOnboarded(){
        String initiativeId = " INITIATIVEID";
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTO.builder()
                .transactionId(request.getTransactionId())
                .initiativeId(initiativeId)
                .userId(request.getUserId())
                .status(RewardConstants.REWARD_STATE_REJECTED)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();

        Mockito.when(rewardTrxSynchronousServiceMock.previewTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenThrow(new TransactionSynchronousException(response));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardPreviewPath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(response);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).previewTransaction(Mockito.any(), Mockito.eq(initiativeId));
    }

    @Test
    void postTransactionPreviewError(){
        String initiativeId = " INITIATIVEID";
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        Mockito.when(rewardTrxSynchronousServiceMock.previewTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenThrow(new RuntimeException());


        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardPreviewPath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError();

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).previewTransaction(Mockito.any(), Mockito.eq(initiativeId));
    }
}