package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.reward.config.ServiceExceptionConfig;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.service.synchronous.RewardTrxSynchronousApiService;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode;
import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionMessage;
@WebFluxTest(controllers = {RewardTrxSynchronousApiController.class})
@Import({ServiceExceptionConfig.class})
class RewardTrxSynchronousApiControllerImplTest {
    private final String rewardPreviewPath = "/reward/initiative/preview/{initiativeId}";
    private final String rewardAuthorizePath = "/reward/initiative/{initiativeId}";

    @MockBean
    private RewardTrxSynchronousApiService rewardTrxSynchronousServiceMock;

    @Autowired
    protected WebTestClient webClient;

    @Test
    void postTransactionPreviewOK(){

        String initiativeId = "INITIATIVEID";
        Reward reward = new Reward();
        reward.setCounters(new RewardCounters());
        reward.getCounters().setVersion(5);
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTO.builder()
                .initiativeId(initiativeId)
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .reward(reward)
                .build();

        Mockito.when(rewardTrxSynchronousServiceMock.previewTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenReturn(Mono.just(response));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardPreviewPath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ETAG, response.getReward().getCounters().getVersion())
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

        Mockito.when(rewardTrxSynchronousServiceMock.previewTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenThrow(new InitiativeNotActiveException(String.format(ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,initiativeId),response));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardPreviewPath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(HttpHeaders.ETAG)
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(response);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).previewTransaction(Mockito.any(), Mockito.eq(initiativeId));
    }

    @Test
    void postTransactionPreviewNotResponse(){
        String initiativeId = " INITIATIVEID";
        ErrorDTO errorDTO = new ErrorDTO(ExceptionCode.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT, String.format(ExceptionMessage.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT_MSG,initiativeId));
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);

        Mockito.when(rewardTrxSynchronousServiceMock.previewTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenReturn(Mono.empty());

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardPreviewPath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class).isEqualTo(errorDTO);

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

    @Test
    void postTransactionAuthorizeOK(){

        String initiativeId = "INITIATIVEID";
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTO.builder()
                .initiativeId(initiativeId)
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .build();

        Mockito.when(rewardTrxSynchronousServiceMock.authorizeTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenReturn(Mono.just(response));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardAuthorizePath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(response);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).authorizeTransaction(Mockito.any(), Mockito.eq(initiativeId));
    }

    @Test
    void postTransactionAuthorizeUserNotOnboarded(){
        String initiativeId = " INITIATIVEID";
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO response = SynchronousTransactionResponseDTO.builder()
                .transactionId(request.getTransactionId())
                .initiativeId(initiativeId)
                .userId(request.getUserId())
                .status(RewardConstants.REWARD_STATE_REJECTED)
                .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
                .build();

        Mockito.when(rewardTrxSynchronousServiceMock.authorizeTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenThrow(new InitiativeNotActiveException(String.format(ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,initiativeId),response));

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardAuthorizePath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(response);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).authorizeTransaction(Mockito.any(), Mockito.eq(initiativeId));
    }

    @Test
    void postTransactionAuthorizeNotResponse(){
        String initiativeId = " INITIATIVEID";
        ErrorDTO errorDTO = new ErrorDTO(ExceptionCode.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT, String.format(ExceptionMessage.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT_MSG,initiativeId));
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        Mockito.when(rewardTrxSynchronousServiceMock.authorizeTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenReturn(Mono.empty());

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardAuthorizePath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDTO.class).isEqualTo(errorDTO);

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).authorizeTransaction(Mockito.any(), Mockito.eq(initiativeId));
    }
    @Test
    void postTransactionAuthorizeError(){
        String initiativeId = " INITIATIVEID";
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        Mockito.when(rewardTrxSynchronousServiceMock.authorizeTransaction(Mockito.any(), Mockito.eq(initiativeId))).thenThrow(new RuntimeException());


        webClient.post()
                .uri(uriBuilder -> uriBuilder.path(rewardAuthorizePath)
                        .build(initiativeId))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError();

        Mockito.verify(rewardTrxSynchronousServiceMock, Mockito.only()).authorizeTransaction(Mockito.any(), Mockito.eq(initiativeId));
    }
}