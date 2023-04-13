package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.service.synchronous.RewardTrxSynchronousApiService;
import it.gov.pagopa.reward.utils.PerformanceLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class RewardTrxSynchronousApiControllerImpl implements RewardTrxSynchronousApiController {
    private final RewardTrxSynchronousApiService rewardTrxSynchronousService;

    public RewardTrxSynchronousApiControllerImpl(RewardTrxSynchronousApiService rewardTrxSynchronousService) {
        this.rewardTrxSynchronousService = rewardTrxSynchronousService;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId) {
        log.info("[SYNC_PREVIEW_TRANSACTION] The user {} requests preview of a transaction", trxPreviewRequest.getUserId());

        return PerformanceLogger.logTimingFinally("[SYNC_PREVIEW_TRANSACTION]",
                rewardTrxSynchronousService.previewTransaction(trxPreviewRequest, initiativeId)
                        .switchIfEmpty(Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND,"NOTFOUND"))),
                trxPreviewRequest.toString());
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> authorizeTransaction(SynchronousTransactionRequestDTO trxAuthorizeRequest, String initiativeId) {
        return rewardTrxSynchronousService.authorizeTransaction(trxAuthorizeRequest, initiativeId);
    }
}
