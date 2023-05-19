package it.gov.pagopa.reward.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.common.web.exception.Severity;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.controller.RewardTrxSynchronousApiController;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionResponseDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;

class ErrorManagerTest extends BaseIntegrationTest {

    @SpyBean
    private RewardTrxSynchronousApiController rewardTrxSynchronousApiController;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void handleExceptionClientExceptionNoBody() {
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);

        Mockito.doThrow(new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "Cannot find initiative having id " + "ClientExceptionNoBody"))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("ClientExceptionNoBody"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("ClientExceptionNoBody"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().isEmpty();
    }

    @Test
    void handleExceptionClientExceptionWithBody(){
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);

        Mockito.doThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody"))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("ClientExceptionWithBody"));

        ErrorDTO errorClientExceptionWithBody= new ErrorDTO(Severity.ERROR,"Error","Error ClientExceptionWithBody");

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("ClientExceptionWithBody"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBody);


        Mockito.doThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody", new Throwable()))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(),Mockito.eq("ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable"));
        ErrorDTO errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable= new ErrorDTO(Severity.ERROR,"Error","Error ClientExceptionWithBody");

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("ClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable);
    }

    @Test
    void handleExceptionClientExceptionTest(){
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        ErrorDTO expectedErrorClientException = new ErrorDTO(Severity.ERROR,"Error","Something gone wrong");

        Mockito.doThrow(ClientException.class)
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(),Mockito.eq("ClientException"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("ClientException"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);


        Mockito.doThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus and message"))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(),Mockito.eq("ClientExceptionStatusAndMessage"));
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("ClientExceptionStatusAndMessage"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);

       Mockito.doThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus, message and throwable", new Throwable()))
               .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("ClientExceptionStatusAndMessageAndThrowable"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("ClientExceptionStatusAndMessageAndThrowable"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);
    }

    @Test
    void handleExceptionRuntimeException(){
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        ErrorDTO expectedErrorDefault = new ErrorDTO(Severity.ERROR,"Error","Something gone wrong");

        Mockito.doThrow(RuntimeException.class)
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(),Mockito.eq("RuntimeException"));
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("RuntimeException"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDefault);
    }

    @Test
    void SynchronousHandleExceptionClientExceptionInitiativeNotFound() {
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTOFaker.mockInstance(1);
        responseDTO.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND));

        Mockito.doThrow(new TransactionSynchronousException(responseDTO))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("TransactionSynchronousExceptionInitiativeNotFound"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("TransactionSynchronousExceptionInitiativeNotFound"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(responseDTO);
    }

    @Test
    void SynchronousHandleExceptionClientExceptionInitiativeNotOnboarded() {
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTOFaker.mockInstance(1);
        responseDTO.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));

        Mockito.doThrow(new TransactionSynchronousException(responseDTO))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("TransactionSynchronousExceptionNotOnboarded"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("TransactionSynchronousExceptionNotOnboarded"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(responseDTO);
    }

    @Test
    void SynchronousHandleExceptionClientExceptionInitiativeInternalServerError() {
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTOFaker.mockInstance(1);
        responseDTO.setRejectionReasons(List.of("ANOTHER_REJECTION"));

        Mockito.doThrow(new TransactionSynchronousException(responseDTO))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("TransactionSynchronousExceptionInternalServerError"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
                        .build("TransactionSynchronousExceptionInternalServerError"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(responseDTO);
    }
}