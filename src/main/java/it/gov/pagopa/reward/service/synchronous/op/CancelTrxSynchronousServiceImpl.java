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

    private static final BigDecimal BIG_DECIMAL_ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
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
        log.trace("[SYNC_CANCEL_TRANSACTION] Starting cancel transaction {} having reward {}", trxCancelRequest.getTransactionId(), trxCancelRequest.getRewardCents());
        TransactionDTO trxDTO = syncTrxRequest2TransactionDtoMapper.apply(trxCancelRequest);

        Mono<Pair<InitiativeConfig, OnboardingInfo>> trxChecks = checkInitiative(trxCancelRequest, initiativeId)
                .flatMap(b -> checkOnboarded(trxCancelRequest, trxDTO, initiativeId))
                .map(initiative2OnboardingInfo -> {
                    transformIntoRefundTrx(trxDTO, initiativeId, initiative2OnboardingInfo.getFirst().getOrganizationId(), trxCancelRequest.getRewardCents());
                    return initiative2OnboardingInfo;
                });


        return trxChecks
                .flatMap(i2o -> findUserInitiativeCounter(initiativeId, i2o, trxDTO))
                .switchIfEmpty(Mono.error(new InitiativeNotActiveException(String.format(RewardConstants.ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,initiativeId),syncTrxRequest2TransactionDtoMapper
                        .apply(trxCancelRequest, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE)))))
                .flatMap(counters -> checkCounterOrEvaluateThenUpdate(trxDTO, initiativeId, new UserInitiativeCountersWrapper(counters.getEntityId(), new HashMap<>(Map.of(initiativeId, counters))), trxCancelRequest.getRewardCents()))
                .map(rewardTransaction -> mapper.apply(trxDTO.getId(), initiativeId, rewardTransaction));
    }


    public void transformIntoRefundTrx(TransactionDTO trx, String initiativeId, String organizationId, long rewardCents) {
        BigDecimal chargeRewardEur = CommonUtilities.centsToEuro(rewardCents);

        TransactionProcessed chargeTrx = trx2processed(trx, initiativeId, organizationId, chargeRewardEur);

        // refund info
        trx.setOperationType(refundOperationType);
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.setTrxDate(OffsetDateTime.now());
        trx.setEffectiveAmount(BIG_DECIMAL_ZERO);

        Map<String, RefundInfo.PreviousReward> previousRewardMap = Map.of(
                initiativeId,
                new RefundInfo.PreviousReward(initiativeId, organizationId, chargeRewardEur));

        trx.setRefundInfo(new RefundInfo(List.of(chargeTrx), previousRewardMap));

    }

    public Mono<RewardTransactionDTO> checkCounterOrEvaluateThenUpdate(TransactionDTO trxDTO, String initiativeId, UserInitiativeCountersWrapper counters, long rewardCents) {
        UserInitiativeCounters counter = counters.getInitiatives().get(initiativeId);

        if(counter.getPendingTrx()!=null){
            return Mono.error(new PendingCounterException());
        } else {
            return handleUnlockedCounterForCancelTrx("SYNC_CANCEL_TRANSACTION", trxDTO, initiativeId, counters, rewardCents)
                    .flatMap(ctr2reward -> userInitiativeCountersRepository.save(counter)
                            .map(x -> ctr2reward.getSecond())
                    );
        }
    }

    public Mono<Pair<UserInitiativeCountersWrapper,RewardTransactionDTO>> handleUnlockedCounterForCancelTrx(String flowName, TransactionDTO trxDTO,String initiativeId, UserInitiativeCountersWrapper counters, long rewardCents){
        return handleUnlockedCounter(flowName, trxDTO, initiativeId, counters, -rewardCents);
    }

    protected TransactionProcessed trx2processed(TransactionDTO trx, String initiativeId, String organizationId, BigDecimal rewardEur){
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
        out.setRewards(Map.of(initiativeId, new Reward(initiativeId, organizationId, rewardEur)));
        out.setInitiativeRejectionReasons(new HashMap<>());
        out.setStatus(RewardConstants.REWARD_STATE_REWARDED);
        out.setOperationTypeTranscoded(OperationType.CHARGE);
        return  out;
    }
}
