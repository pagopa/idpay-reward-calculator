package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import reactor.core.publisher.Mono;

public interface CreateTrxSynchronousService {
    Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId);
    Mono<SynchronousTransactionResponseDTO> authorizeTransaction(SynchronousTransactionAuthRequestDTO trxAuthorizeRequest, String initiativeId, long counterVersion);
}
