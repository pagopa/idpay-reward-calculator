package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewRequest;
import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/reward")
public interface RewardTrxSynchronousApiController {

    @PostMapping("/preview/{initiativeId}")
    Mono<TransactionPreviewResponse> postTransactionPreview(@RequestBody TransactionPreviewRequest trxPreviewRequest,
                                                            @PathVariable("initiativeId") String initiativeId);
}
