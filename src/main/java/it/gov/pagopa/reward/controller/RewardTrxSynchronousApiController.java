package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/idpay/reward")
public interface RewardTrxSynchronousApiController {

    @PostMapping("/preview/{initiativeId}")
    Mono<SynchronousTransactionResponseDTO> postTransactionPreview(@RequestBody SynchronousTransactionRequestDTO trxPreviewRequest,
                                                                   @PathVariable("initiativeId") String initiativeId);
}
