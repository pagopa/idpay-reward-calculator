package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import reactor.core.publisher.Mono;

public interface RewardTrxSynchronousApiService {
    Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId);

    Mono<SynchronousTransactionResponseDTO> authorizeTransaction(SynchronousTransactionRequestDTO trxAuthorizeRequest, String initiativeId);
}
