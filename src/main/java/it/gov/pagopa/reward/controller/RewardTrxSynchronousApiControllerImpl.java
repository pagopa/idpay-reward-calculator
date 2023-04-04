package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewRequest;
import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewResponse;
import it.gov.pagopa.reward.service.synchronous.RewardTrxSynchronousApiApiServiceImpl;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class RewardTrxSynchronousApiControllerImpl implements RewardTrxSynchronousApiController {
    private final RewardTrxSynchronousApiApiServiceImpl rewardTrxSynchronousService;

    public RewardTrxSynchronousApiControllerImpl(RewardTrxSynchronousApiApiServiceImpl rewardTrxSynchronousService) {
        this.rewardTrxSynchronousService = rewardTrxSynchronousService;
    }

    @Override
    public Mono<TransactionPreviewResponse> postTransactionPreview(TransactionPreviewRequest trxPreviewRequest, String initiativeId) {
        return rewardTrxSynchronousService.postTransactionPreview(trxPreviewRequest,initiativeId);

    }
}
