package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.TransactionProcessed2SyncTrxResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.synchronous.op.recover.HandleSyncCounterUpdatingTrxService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CancelTrxSynchronousServiceImpl extends BaseTrxSynchronousOp implements CancelTrxSynchronousService {

    private final String refundOperationType;
    private final TransactionProcessedRepository transactionProcessedRepository;

    public CancelTrxSynchronousServiceImpl(
            @Value("${app.operationType.refund") String refundOperationType,

            TransactionProcessedRepository transactionProcessedRepository,
            UserInitiativeCountersRepository userInitiativeCountersRepository,
            HandleSyncCounterUpdatingTrxService handleSyncCounterUpdatingTrxService,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper,
            TransactionProcessed2SyncTrxResponseDTOMapper syncTrxResponseDTOMapper, RewardContextHolderService rewardContextHolderService) {
        super(transactionProcessedRepository, syncTrxResponseDTOMapper, userInitiativeCountersRepository, handleSyncCounterUpdatingTrxService, initiativesEvaluatorFacadeService, rewardTransaction2SynchronousTransactionResponseDTOMapper, rewardContextHolderService);

        this.refundOperationType = refundOperationType;

        this.transactionProcessedRepository = transactionProcessedRepository;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> cancelTransaction(String trxId) {
        return transactionProcessedRepository.findById(trxId)
                .flatMap(trx -> {
                    log.debug("[SYNC_CANCEL_TRANSACTION] Transaction to be refunded has been found: {}", trxId);
                    if(trx.getRewards()==null || trx.getRewards().size()==0){
                        return Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "REJECTED authorization"));
                    }
                    else if(trx.getRewards().size()>1){
                        return Mono.error(new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "Cannot cancel transaction: it was rewarded more than once!%s: %s".formatted(trxId, trx.getRewards().keySet())));
                    }

                    Reward reward = trx.getRewards().values().iterator().next();

                    return checkSyncTrxAlreadyProcessed(buildRefundId(trxId), reward.getInitiativeId())

                            .switchIfEmpty(Mono.defer(() -> refundTransaction((TransactionProcessed) trx, reward)));
                });
    }

    private String buildRefundId(String trxId) {
        return trxId + RewardConstants.SYNC_TRX_REFUND_ID_SUFFIX;
    }

    private Mono<SynchronousTransactionResponseDTO> refundTransaction(TransactionProcessed trx, Reward reward) {
        log.debug("[SYNC_CANCEL_TRANSACTION] Refunding transaction {}", trx.getId());

        TransactionDTO refundTrx = buildRefundTrx(trx, reward.getInitiativeId());

        return lockCounterAndEvaluate(retrieveInitiative2OnboardingInfo(reward), refundTrx, reward.getInitiativeId());
    }

    public TransactionDTO buildRefundTrx(TransactionProcessed trx, String initiativeId) {
        TransactionDTO refundTrx = new TransactionDTO();

        refundTrx.setIdTrxAcquirer(trx.getIdTrxAcquirer());
        refundTrx.setAcquirerCode(trx.getAcquirerCode());
        refundTrx.setAcquirerId(trx.getAcquirerId());
        refundTrx.setUserId(trx.getUserId());
        refundTrx.setCorrelationId(trx.getCorrelationId());
        refundTrx.setTrxChargeDate(trx.getTrxChargeDate().atZone(CommonConstants.ZONEID).toOffsetDateTime());
        refundTrx.setRejectionReasons(trx.getRejectionReasons());
        refundTrx.setChannel(trx.getChannel());

        // refund info
        refundTrx.setId(buildRefundId(trx.getId()));
        refundTrx.setOperationType(refundOperationType);
        refundTrx.setOperationTypeTranscoded(OperationType.REFUND);
        refundTrx.setTrxDate(OffsetDateTime.now());
        refundTrx.setAmountCents(trx.getAmountCents());
        refundTrx.setAmount(trx.getAmount());
        refundTrx.setEffectiveAmount(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));

        Map<String, RefundInfo.PreviousReward> previousRewardMap = null;
        Reward reward = trx.getRewards().get(initiativeId);
        if(reward != null) {
            previousRewardMap = Map.of(initiativeId, new RefundInfo.PreviousReward(reward.getInitiativeId(), reward.getOrganizationId(), reward.getAccruedReward()));
        }
        refundTrx.setRefundInfo(new RefundInfo(List.of(trx), previousRewardMap));
        return refundTrx;
    }

}
