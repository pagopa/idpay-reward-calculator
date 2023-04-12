package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.service.synchronous.RewardTrxSynchronousApiService;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class RewardTrxSynchronousApiControllerImpl implements RewardTrxSynchronousApiController {
    private final RewardTrxSynchronousApiService rewardTrxSynchronousService;

    public RewardTrxSynchronousApiControllerImpl(RewardTrxSynchronousApiService rewardTrxSynchronousService) {
        this.rewardTrxSynchronousService = rewardTrxSynchronousService;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId) {
        return rewardTrxSynchronousService.previewTransaction(trxPreviewRequest,initiativeId);

    }
}
