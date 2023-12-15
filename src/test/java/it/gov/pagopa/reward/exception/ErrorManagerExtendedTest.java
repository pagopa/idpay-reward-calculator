package it.gov.pagopa.reward.exception;

import com.jayway.jsonpath.Criteria;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.controller.RewardTrxSynchronousApiController;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotFoundOrNotDiscountException;
import it.gov.pagopa.reward.exception.custom.InternalServerErrorException;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
import it.gov.pagopa.reward.test.fakers.SynchronousTransactionResponseDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;

class ErrorManagerExtendedTest extends BaseIntegrationTest {

    @SpyBean
    private RewardTrxSynchronousApiController rewardTrxSynchronousApiController;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper;

    @Test
    void SynchronousHandleExceptionClientExceptionInitiativeNotFound() {
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTOFaker.mockInstance(1);
        responseDTO.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND));

        Mockito.doThrow(new InitiativeNotFoundOrNotDiscountException(String.format(RewardConstants.ExceptionMessage.INITIATIVE_NOT_READY_MSG, "TransactionSynchronousExceptionInitiativeNotFound"),responseDTO))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("TransactionSynchronousExceptionInitiativeNotFound"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/initiative/preview/{initiativeId}")
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


        Mockito.doThrow(new InitiativeNotActiveException(String.format(RewardConstants.ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,"TransactionSynchronousExceptionNotOnboarded"),responseDTO))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("TransactionSynchronousExceptionNotOnboarded"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/initiative/preview/{initiativeId}")
                        .build("TransactionSynchronousExceptionNotOnboarded"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(responseDTO);
    }

    //TODO: check this test
    @Test
    void SynchronousHandleExceptionClientExceptionInitiativeInternalServerError() {
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTOFaker.mockInstance(1);
        responseDTO.setRejectionReasons(List.of("ANOTHER_REJECTION"));

        Mockito.doThrow(new InternalServerErrorException(RewardConstants.ExceptionMessage.GENERIC_ERROR_MSG,responseDTO))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("TransactionSynchronousExceptionInternalServerError"));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/initiative/preview/{initiativeId}")
                        .build("TransactionSynchronousExceptionInternalServerError"))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(responseDTO);
    }
}