//package it.gov.pagopa.reward.exception;
//
//import it.gov.pagopa.reward.BaseIntegrationTest;
//import it.gov.pagopa.reward.controller.RewardTrxSynchronousApiController;
//import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
//import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
//import it.gov.pagopa.reward.test.fakers.SynchronousTransactionRequestDTOFaker;
//import it.gov.pagopa.reward.test.fakers.SynchronousTransactionResponseDTOFaker;
//import it.gov.pagopa.reward.utils.RewardConstants;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.mock.mockito.SpyBean;
//import org.springframework.http.HttpStatus;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.springframework.web.reactive.function.BodyInserters;
//
//import java.util.List;
//
//class TransactionSynchronousExceptionHandlerTest extends BaseIntegrationTest {
//    @SpyBean
//    RewardTrxSynchronousApiController rewardTrxSynchronousApiController;
//
//    @Autowired
//    WebTestClient webTestClient;
//
//
//    @Test
//    void handleExceptionClientExceptionNoBody() {
//        SynchronousTransactionRequestDTO request = SynchronousTransactionRequestDTOFaker.mockInstance(1);
//        SynchronousTransactionResponseDTO responseDTO = SynchronousTransactionResponseDTOFaker.mockInstance(1);
//        responseDTO.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND));
//
//        Mockito.doThrow(new TransactionSynchronousException(responseDTO))
//                .when(rewardTrxSynchronousApiController).previewTransaction(Mockito.any(), Mockito.eq("TransactionSynchronousException"));
//
//        webTestClient.post()
//                .uri(uriBuilder -> uriBuilder.path("/reward/preview/{initiativeId}")
//                        .build("TransactionSynchronousException"))
//                .body(BodyInserters.fromValue(request))
//                .exchange()
//                .expectStatus().isNotFound()
//                .expectBody(SynchronousTransactionResponseDTO.class).isEqualTo(responseDTO);
//    }
//
//}