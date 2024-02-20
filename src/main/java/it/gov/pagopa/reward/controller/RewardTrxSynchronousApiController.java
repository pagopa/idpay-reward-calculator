package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/reward")
public interface RewardTrxSynchronousApiController {

    @PostMapping("/initiative/preview/{initiativeId}")
    Mono<ResponseEntity<SynchronousTransactionResponseDTO>> previewTransaction(@RequestBody SynchronousTransactionRequestDTO trxPreviewRequest,
                                                                               @PathVariable("initiativeId") String initiativeId);

    @PostMapping("/initiative/{initiativeId}")
    Mono<SynchronousTransactionResponseDTO> authorizeTransaction(@RequestHeader(HttpHeaders.IF_MATCH) long counterVersion,
                                                                 @RequestBody SynchronousTransactionAuthRequestDTO trxAuthorizeRequest,
                                                                 @PathVariable("initiativeId") String initiativeId);
    @DeleteMapping("/{transactionId}") //TODO IDP-2357 cambia il path
    Mono<SynchronousTransactionResponseDTO> cancelTransaction(@RequestBody SynchronousTransactionAuthRequestDTO trxCancelRequest,
                                                              @PathVariable("initiativeId") String initiativeId);
}
