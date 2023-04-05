package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousRequest;
import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousResponse;
import reactor.core.publisher.Mono;

public interface RewardTrxSynchronousApiService {
    Mono<TransactionSynchronousResponse> postTransactionPreview(TransactionSynchronousRequest trxPreviewRequest, String initiativeId);
}
