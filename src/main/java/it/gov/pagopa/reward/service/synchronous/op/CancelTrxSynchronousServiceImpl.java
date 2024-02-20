package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.exception.custom.PendingCounterException;
import it.gov.pagopa.reward.model.OnboardingInfo;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CancelTrxSynchronousServiceImpl extends BaseTrxSynchronousOp implements CancelTrxSynchronousService {

    private final String refundOperationType;

    public CancelTrxSynchronousServiceImpl(
            @Value("${app.operationType.refund") String refundOperationType,

            UserInitiativeCountersRepository userInitiativeCountersRepository,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper,
            RewardContextHolderService rewardContextHolderService,
            Transaction2RewardTransactionMapper rewardTransactionMapper,
            UserInitiativeCountersUpdateService userInitiativeCountersUpdateService,
            OnboardedInitiativesService onboardedInitiativesService,
            SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper) {
        super(
                userInitiativeCountersRepository,
                initiativesEvaluatorFacadeService,
                rewardTransaction2SynchronousTransactionResponseDTOMapper,
                rewardContextHolderService,
                rewardTransactionMapper,
                userInitiativeCountersUpdateService,
                onboardedInitiativesService,
                syncTrxRequest2TransactionDtoMapper);
        this.refundOperationType = refundOperationType;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> cancelTransaction(SynchronousTransactionAuthRequestDTO trxCancelRequest, String initiativeId) {
        // request as input also trxChargeDate and rewardCents
        log.trace("[SYNC_CANCEL_TRANSACTION] Starting cancel transaction {} having reward {}", trxCancelRequest.getTransactionId(), trxCancelRequest.getRewardCents());
        TransactionDTO trxDTO = syncTrxRequest2TransactionDtoMapper.apply(trxCancelRequest);

        Mono<Pair<InitiativeConfig, OnboardingInfo>> trxChecks = checkInitiative(trxCancelRequest, initiativeId)
                .flatMap(b -> checkOnboarded(trxCancelRequest, trxDTO, initiativeId))
                .doOnNext(initiative2OnboardingInfo -> buildRefundTrx(trxDTO, initiativeId, initiative2OnboardingInfo.getFirst().getOrganizationId(), trxCancelRequest.getRewardCents()));


        return trxChecks
                .flatMap(i2o -> findUserInitiativeCounter(initiativeId, i2o, trxDTO))
                .switchIfEmpty(Mono.error(new InitiativeNotActiveException(String.format(RewardConstants.ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,initiativeId),syncTrxRequest2TransactionDtoMapper
                        .apply(trxCancelRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE)))))
                .flatMap(counters -> checkCounterOrEvaluateThenUpdate(trxDTO, initiativeId, new UserInitiativeCountersWrapper(counters.getEntityId(), new HashMap<>(Map.of(initiativeId, counters))), trxCancelRequest.getRewardCents()))
                .map(rewardTransaction -> mapper.apply(trxDTO.getId(), initiativeId, rewardTransaction));


        // retrieve counter
        // if pending BaseTrxSynchronousOp.handlePendingCounter
        // this.buildRefundTrx
        // BaseTrxSynchronousOp.handleUnlockedCounter
        // BaseTrxSynchronousOp.lockCounter
//        return Mono.error(new UnsupportedOperationException("Refund operation not implemented"));
    }

    // build using just new inputs, setting actual Reward to -1*rewardCents
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
        refundTrx.setId(trx.getId());
        refundTrx.setOperationType(refundOperationType);
        refundTrx.setOperationTypeTranscoded(OperationType.REFUND);
        refundTrx.setTrxDate(OffsetDateTime.now());
        refundTrx.setAmountCents(trx.getAmountCents());
        refundTrx.setAmount(trx.getAmount());
        refundTrx.setEffectiveAmount(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));

        Map<String, RefundInfo.PreviousReward> previousRewardMap = null;
        Reward reward = trx.getRewards().get(initiativeId);
        if (reward != null) {
            previousRewardMap = Map.of(initiativeId, new RefundInfo.PreviousReward(reward.getInitiativeId(), reward.getOrganizationId(), reward.getAccruedReward()));
        }
        refundTrx.setRefundInfo(new RefundInfo(List.of(trx), previousRewardMap));
        return refundTrx;
    }

    public void buildRefundTrx(TransactionDTO trx, String initiativeId, String organizationId, long rewardCents) {

        // refund info
        trx.setOperationType(refundOperationType);
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.setEffectiveAmount(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));

        Map<String, RefundInfo.PreviousReward> previousRewardMap = Map.of(
                initiativeId,
                new RefundInfo.PreviousReward(initiativeId, organizationId, CommonUtilities.centsToEuro(rewardCents))); //TODO check

        trx.setRefundInfo(new RefundInfo(List.of(trx2processed(trx, initiativeId)), previousRewardMap));

    }

    private Mono<RewardTransactionDTO> checkCounterOrEvaluateThenUpdate(TransactionDTO trxDTO, String initiativeId, UserInitiativeCountersWrapper counters, long rewardCents) {
        UserInitiativeCounters counter = counters.getInitiatives().get(initiativeId);

        Mono<Pair<UserInitiativeCountersWrapper, RewardTransactionDTO>> ctr2rewardMono;
        if(counter.getPendingTrx()!=null){
            return Mono.error(new PendingCounterException());
        } else {
            ctr2rewardMono = handleUnlockedCounter("SYNC_CANCEL_TRANSACTION", trxDTO, initiativeId, counters, rewardCents);
        }

        return lockCounter(ctr2rewardMono, trxDTO, counter);
    }

    @Override
    protected Mono<RewardTransactionDTO> lockCounter(Mono<Pair<UserInitiativeCountersWrapper, RewardTransactionDTO>> ctr2rewardMono, TransactionDTO trxDTO, UserInitiativeCounters counter) {
        return ctr2rewardMono
                .flatMap(ctr2reward -> userInitiativeCountersRepository.save(counter)
                        .map(x -> ctr2reward.getSecond())
                );
    }

    TransactionProcessed trx2processed(TransactionDTO trx, String initiativeId){
        TransactionProcessed out = new TransactionProcessed();

        out.setId(trx.getId());
        out.setIdTrxAcquirer(trx.getIdTrxAcquirer());
        out.setAcquirerCode(trx.getAcquirerCode());
        out.setTrxDate(trx.getTrxDate().atZoneSameInstant(CommonConstants.ZONEID).toLocalDateTime());
        out.setOperationType(trx.getOperationType());
        out.setAcquirerId(trx.getAcquirerId());
        out.setUserId(trx.getUserId());
        out.setCorrelationId(trx.getCorrelationId());
        out.setAmount(trx.getAmount());
        out.setAmountCents(trx.getAmountCents());
        out.setEffectiveAmount(trx.getEffectiveAmount());
        out.setTrxChargeDate(trx.getTrxChargeDate().atZoneSameInstant(CommonConstants.ZONEID).toLocalDateTime());
        out.setOperationTypeTranscoded(trx.getOperationTypeTranscoded());
        out.setRejectionReasons(trx.getRejectionReasons());
        out.setRefundInfo(trx.getRefundInfo());
        out.setChannel(trx.getChannel());
        out.setRuleEngineTopicPartition(trx.getRuleEngineTopicPartition());
        out.setRuleEngineTopicOffset(trx.getRuleEngineTopicOffset());
        out.setInitiatives(List.of(initiativeId));
        return  out;
    }
}
