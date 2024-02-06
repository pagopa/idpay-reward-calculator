package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/reward")
public interface RewardTrxSynchronousApiController {

    @PostMapping("/initiative/preview/{initiativeId}")
    Mono<ResponseEntity<SynchronousTransactionResponseDTO>> previewTransaction(@RequestBody SynchronousTransactionRequestDTO trxPreviewRequest,
                                                                               @PathVariable("initiativeId") String initiativeId);

    @PostMapping("/initiative/{initiativeId}")
    Mono<SynchronousTransactionResponseDTO> authorizeTransaction(@RequestBody SynchronousTransactionRequestDTO trxAuthorizeRequest,
                                                               @PathVariable("initiativeId") String initiativeId);
    @DeleteMapping("/{transactionId}")
    Mono<SynchronousTransactionResponseDTO> cancelTransaction(@PathVariable("transactionId") String trxId);
}
