package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.ErrorDTO;
import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousRequest;
import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousResponse;
import it.gov.pagopa.reward.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.exception.ClientExceptionWithBody;
import it.gov.pagopa.reward.exception.Severity;
import it.gov.pagopa.reward.service.synchronous.RewardTrxSynchronousApiService;
import it.gov.pagopa.reward.test.fakers.TransactionPreviewRequestFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {RewardTrxSynchronousApiController.class})
class RewardTrxSynchronousApiControllerImplTest {

    @MockBean
    RewardTrxSynchronousApiService rewardTrxSynchronousServiceMock;

    @Autowired
    protected WebTestClient webClient;

    @Test
    void postTransactionPreviewOK(){

        String initiativeId = "INITIATIVEID";
        TransactionSynchronousRequest request = TransactionPreviewRequestFaker.mockInstance(1);
        TransactionSynchronousResponse response = TransactionSynchronousResponse.builder()
                .initiativeId(initiativeId)
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .build();

        Mockito.when(rewardTrxSynchronousServiceMock.postTransactionPreview(Mockito.any(), Mockito.eq(initiativeId))).thenReturn(Mono.just(response));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TransactionSynchronousResponse.class).isEqualTo(response);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).postTransactionPreview(Mockito.any(), Mockito.eq(initiativeId));
    }

    @Test
    void postTransactionPreviewUserNotOnboarded(){
        String initiativeId = " INITIATIVEID";
        TransactionSynchronousRequest request = TransactionPreviewRequestFaker.mockInstance(1);
        Mockito.when(rewardTrxSynchronousServiceMock.postTransactionPreview(Mockito.any(), Mockito.eq(initiativeId))).thenThrow(new ClientExceptionWithBody(HttpStatus.FORBIDDEN,"Error",  "User not onboarded to initiative %s".formatted(initiativeId)));

        ErrorDTO expectedErrorDTO = new ErrorDTO(Severity.ERROR,"Error", "User not onboarded to initiative %s".formatted(initiativeId));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDTO);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).postTransactionPreview(Mockito.any(), Mockito.eq(initiativeId));
    }

    @Test
    void postTransactionPreview(){
        String initiativeId = " INITIATIVEID";
        TransactionSynchronousRequest request = TransactionPreviewRequestFaker.mockInstance(1);
        Mockito.when(rewardTrxSynchronousServiceMock.postTransactionPreview(Mockito.any(), Mockito.eq(initiativeId))).thenThrow(new RuntimeException());

        ErrorDTO expectedDefaultErrorDTO =new ErrorDTO(Severity.ERROR, "Error", "Something gone wrong");
        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(ErrorDTO.class).isEqualTo(expectedDefaultErrorDTO);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).postTransactionPreview(Mockito.any(), Mockito.eq(initiativeId));
    }

    @Test
    void postTransactionPreviewWithNoBodyException(){
        String initiativeId = " INITIATIVEID";
        TransactionSynchronousRequest request = TransactionPreviewRequestFaker.mockInstance(1);
        Mockito.when(rewardTrxSynchronousServiceMock.postTransactionPreview(Mockito.any(), Mockito.eq(initiativeId))).thenThrow(new ClientExceptionNoBody(HttpStatus.BAD_REQUEST));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest();

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).postTransactionPreview(Mockito.any(), Mockito.eq(initiativeId));
    }

}