package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.exception.custom.InitiativeNotFoundOrNotDiscountException;
import it.gov.pagopa.reward.exception.custom.TransactioNotFoundException;
import it.gov.pagopa.reward.service.synchronous.RewardTrxSynchronousApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionMessage;

@RestController
@Slf4j
public class RewardTrxSynchronousApiControllerImpl implements RewardTrxSynchronousApiController {
    private final RewardTrxSynchronousApiService rewardTrxSynchronousService;

    public RewardTrxSynchronousApiControllerImpl(RewardTrxSynchronousApiService rewardTrxSynchronousService) {
        this.rewardTrxSynchronousService = rewardTrxSynchronousService;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId) {
        log.info("[SYNC_PREVIEW_TRANSACTION] The user {} requests preview of a transaction having id {} on initiativeId {}", trxPreviewRequest.getUserId(), trxPreviewRequest.getTransactionId(), initiativeId);

        return PerformanceLogger.logTimingFinally("SYNC_PREVIEW_TRANSACTION",
                rewardTrxSynchronousService.previewTransaction(trxPreviewRequest, initiativeId)
                        .doOnNext(r -> log.info("[SYNC_PREVIEW_TRANSACTION] The preview requested by userId {} of a transaction having id {} on initiativeId {} resulted into status {} and rejections {}", trxPreviewRequest.getUserId(), trxPreviewRequest.getTransactionId(), initiativeId, r.getStatus(), r.getRejectionReasons()))
                        .switchIfEmpty(Mono.error(new InitiativeNotFoundOrNotDiscountException(String.format(ExceptionMessage.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT_MSG, initiativeId)))),
                trxPreviewRequest.toString());
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> authorizeTransaction(SynchronousTransactionRequestDTO trxAuthorizeRequest, String initiativeId) {
        log.info("[SYNC_AUTHORIZE_TRANSACTION] The user {} requests authorize transaction {} on initiativeId {}", trxAuthorizeRequest.getUserId(), trxAuthorizeRequest.getTransactionId(), initiativeId);

        return PerformanceLogger.logTimingFinally("SYNC_AUTHORIZE_TRANSACTION",
                rewardTrxSynchronousService.authorizeTransaction(trxAuthorizeRequest, initiativeId)
                        .doOnNext(r -> log.info("[SYNC_AUTHORIZE_TRANSACTION] The authorization requested by userId {} of a transaction having id {} on initiativeId {} resulted into status {} and rejections {}", trxAuthorizeRequest.getUserId(), trxAuthorizeRequest.getTransactionId(), initiativeId, r.getStatus(), r.getRejectionReasons()))
                        .switchIfEmpty(Mono.error(new InitiativeNotFoundOrNotDiscountException(String.format(ExceptionMessage.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT_MSG, initiativeId)))),
                trxAuthorizeRequest.toString());
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> cancelTransaction(String trxId) {
        log.info("[SYNC_CANCEL_TRANSACTION] Requesting to cancel transaction {}", trxId);

        return PerformanceLogger.logTimingFinally("SYNC_CANCEL_TRANSACTION",
                rewardTrxSynchronousService.cancelTransaction(trxId)
                        .switchIfEmpty(Mono.error(new TransactioNotFoundException(String.format(ExceptionMessage.TRANSACTION_NOT_FOUND_MSG, trxId)))),
                trxId);
    }
}
