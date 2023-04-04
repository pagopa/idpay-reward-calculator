package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewRequest;
import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewResponse;
import reactor.core.publisher.Mono;

public interface RewardTrxSynchronousApiService {
    Mono<TransactionPreviewResponse> postTransactionPreview(TransactionPreviewRequest trxPreviewRequest, String initiativeId);
}
