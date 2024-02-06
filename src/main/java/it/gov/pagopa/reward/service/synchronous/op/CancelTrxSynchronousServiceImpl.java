package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    public CancelTrxSynchronousServiceImpl(
            @Value("${app.operationType.refund") String refundOperationType,

            UserInitiativeCountersRepository userInitiativeCountersRepository,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper,
            RewardContextHolderService rewardContextHolderService,
            Transaction2RewardTransactionMapper rewardTransactionMapper,
            UserInitiativeCountersUpdateService userInitiativeCountersUpdateService) {
        super(
                userInitiativeCountersRepository,
                initiativesEvaluatorFacadeService,
                rewardTransaction2SynchronousTransactionResponseDTOMapper,
                rewardContextHolderService,
                rewardTransactionMapper,
                userInitiativeCountersUpdateService
        );
        this.refundOperationType = refundOperationType;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> cancelTransaction(String trxId) {
        // request as input also trxChargeDate and rewardCents
        // retrieve counter
        // if pending BaseTrxSynchronousOp.handlePendingCounter
        // this.buildRefundTrx
        // BaseTrxSynchronousOp.handleUnlockedCounter
        // BaseTrxSynchronousOp.lockCounter
        return Mono.error(new UnsupportedOperationException("Refund operation not implemented"));
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
        if(reward != null) {
            previousRewardMap = Map.of(initiativeId, new RefundInfo.PreviousReward(reward.getInitiativeId(), reward.getOrganizationId(), reward.getAccruedReward()));
        }
        refundTrx.setRefundInfo(new RefundInfo(List.of(trx), previousRewardMap));
        return refundTrx;
    }

}
