package it.gov.pagopa.reward.exception;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.controller.RewardTrxSynchronousApiController;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotFoundOrNotDiscountException;
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
import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionMessage;

@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"})
class ErrorManagerExtendedTestDeprecated extends BaseIntegrationTest {

    @SpyBean
    private RewardTrxSynchronousApiController rewardTrxSynchronousApiController;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper;
    
    private final String INITIATIVEID_EXCEPTION_NOT_ONBOARDED = "TransactionSynchronousExceptionNotOnboarded";

    private final String INITIATIVEID_EXCEPTION_NOT_FOUND = "TransactionSynchronousExceptionInitiativeNotFound";

    @Test
    void SynchronousHandleExceptionClientExceptionInitiativeNotFound() {
        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTOFaker.mockInstance(1);
        responseDTO.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND));

        Mockito.doThrow(new InitiativeNotFoundOrNotDiscountException(String.format(ExceptionMessage.INITIATIVE_NOT_READY_MSG, INITIATIVEID_EXCEPTION_NOT_FOUND),responseDTO))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq(INITIATIVEID_EXCEPTION_NOT_FOUND));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/initiative/preview/{initiativeId}")
                        .build(INITIATIVEID_EXCEPTION_NOT_FOUND))
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


        Mockito.doThrow(new InitiativeNotActiveException(String.format(ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,INITIATIVEID_EXCEPTION_NOT_ONBOARDED),responseDTO))
                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq(INITIATIVEID_EXCEPTION_NOT_ONBOARDED));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/initiative/preview/{initiativeId}")
                        .build(INITIATIVEID_EXCEPTION_NOT_ONBOARDED))
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(responseDTO);
    }
}