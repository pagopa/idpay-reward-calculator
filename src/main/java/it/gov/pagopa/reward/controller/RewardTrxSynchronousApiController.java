package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/reward")
public interface RewardTrxSynchronousApiController {

    @PostMapping("/preview/{initiativeId}")
    Mono<SynchronousTransactionResponseDTO> previewTransaction(@RequestBody SynchronousTransactionRequestDTO trxPreviewRequest,
                                                               @PathVariable("initiativeId") String initiativeId);

    @PostMapping("/{initiativeId}")
    Mono<SynchronousTransactionResponseDTO> authorizeTransaction(@RequestBody SynchronousTransactionRequestDTO trxAuthorizeRequest,
                                                               @PathVariable("initiativeId") String initiativeId);
    @DeleteMapping("/{transactionId}")
    Mono<SynchronousTransactionResponseDTO> cancelTransaction(@PathVariable("transactionId") String trxId);
}
