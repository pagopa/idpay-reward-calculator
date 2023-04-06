package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import reactor.core.publisher.Mono;

public interface RewardTrxSynchronousApiService {
    Mono<SynchronousTransactionResponseDTO> postTransactionPreview(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId);
}
