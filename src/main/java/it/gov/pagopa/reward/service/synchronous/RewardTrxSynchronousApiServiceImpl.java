package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.service.synchronous.op.CancelTrxSynchronousService;
import it.gov.pagopa.reward.service.synchronous.op.CreateTrxSynchronousService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RewardTrxSynchronousApiServiceImpl implements RewardTrxSynchronousApiService {

    private final CreateTrxSynchronousService createTrxSynchronousService;
    private final CancelTrxSynchronousService cancelTrxSynchronousService;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardTrxSynchronousApiServiceImpl(CreateTrxSynchronousService createTrxSynchronousService, CancelTrxSynchronousService cancelTrxSynchronousService) {
        this.createTrxSynchronousService = createTrxSynchronousService;
        this.cancelTrxSynchronousService = cancelTrxSynchronousService;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId) {
        return createTrxSynchronousService.previewTransaction(trxPreviewRequest, initiativeId);
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> authorizeTransaction(SynchronousTransactionAuthRequestDTO trxAuthorizeRequest, String initiativeId, long counterVersion) {
        return createTrxSynchronousService.authorizeTransaction(trxAuthorizeRequest, initiativeId, counterVersion);
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> cancelTransaction(String trxId) {
        return cancelTrxSynchronousService.cancelTransaction(trxId);
    }
}
