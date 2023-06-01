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
import it.gov.pagopa.reward.exception.TransactionSynchronousException;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CancelTrxSynchronousServiceImpl extends BaseTrxSynchronousOp implements CancelTrxSynchronousService {

    private final String refundOperationType;

    private final TransactionProcessedRepository transactionProcessedRepository;

    private final TransactionProcessed2SyncTrxResponseDTOMapper transactionProcessed2SyncTrxResponseDTOMapper;

    public CancelTrxSynchronousServiceImpl(
            @Value("${app.operationType.refund") String refundOperationType,

            TransactionProcessedRepository transactionProcessedRepository,
            UserInitiativeCountersRepository userInitiativeCountersRepository,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper,
            TransactionProcessed2SyncTrxResponseDTOMapper transactionProcessed2SyncTrxResponseDTOMapper) {
        super(userInitiativeCountersRepository, initiativesEvaluatorFacadeService, rewardTransaction2SynchronousTransactionResponseDTOMapper);

        this.refundOperationType = refundOperationType;

        this.transactionProcessedRepository = transactionProcessedRepository;
        this.transactionProcessed2SyncTrxResponseDTOMapper = transactionProcessed2SyncTrxResponseDTOMapper;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> cancelTransaction(String trxId) {
        return transactionProcessedRepository.findById(trxId)
                .flatMap(trx -> {
                    log.debug("[SYNC_CANCEL_TRANSACTION] Transaction to be refunded has been found: {}", trxId);
                    String initiativeId = Optional.ofNullable(trx.getRewards()).filter(map -> !CollectionUtils.isEmpty(map)).map(m -> m.keySet().iterator().next()).orElse(null);

                    if(initiativeId==null){
                        return Mono.error(new ClientExceptionNoBody(HttpStatus.NOT_FOUND, "REJECTED authorization"));
                    }

                    return transactionProcessedRepository.findById(buildRefundId(trxId))
                            .map(refund -> {
                                log.info("[SYNC_CANCEL_TRANSACTION] Transaction has already been refunded: {}", trxId);
                                return transactionProcessed2SyncTrxResponseDTOMapper.apply(refund, initiativeId);
                            })
                            .doOnNext(t -> {throw new TransactionSynchronousException(HttpStatus.CONFLICT,t);})

                            .switchIfEmpty(Mono.defer(() -> refundTransaction((TransactionProcessed) trx, initiativeId)));
                });
    }

    private String buildRefundId(String trxId) {
        return trxId + "_REFUND";
    }

    private Mono<SynchronousTransactionResponseDTO> refundTransaction(TransactionProcessed trx, String initiativeId) {
        log.debug("[SYNC_CANCEL_TRANSACTION] Refunding transaction {}", trx.getId());

        TransactionDTO refundTrx = buildRefundTrx(trx, initiativeId);
        return lockCounterAndEvaluate(Mono.just(refundTrx), refundTrx, initiativeId);
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
