package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.TransactionProcessed2SyncTrxResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.exception.TransactionSynchronousException;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.synchronous.op.recover.HandleSyncCounterUpdatingTrxService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class CreateTrxSynchronousServiceImpl extends BaseTrxSynchronousOp implements CreateTrxSynchronousService {

    private final RewardContextHolderService rewardContextHolderService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public CreateTrxSynchronousServiceImpl(
            RewardContextHolderService rewardContextHolderService,
            OnboardedInitiativesService onboardedInitiativesService,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            TransactionProcessedRepository transactionProcessedRepository,
            UserInitiativeCountersRepository userInitiativeCountersRepository,
            HandleSyncCounterUpdatingTrxService handleSyncCounterUpdatingTrxService,
            SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper,
            RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper,
            TransactionProcessed2SyncTrxResponseDTOMapper syncTrxResponseDTOMapper) {
        super(transactionProcessedRepository, syncTrxResponseDTOMapper, userInitiativeCountersRepository, handleSyncCounterUpdatingTrxService, initiativesEvaluatorFacadeService, rewardTransaction2SynchronousTransactionResponseDTOMapper);

        this.rewardContextHolderService = rewardContextHolderService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.syncTrxRequest2TransactionDtoMapper = syncTrxRequest2TransactionDtoMapper;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId) {
        log.trace("[SYNC_PREVIEW_TRANSACTION] Starting reward preview calculation for transaction {}", trxPreviewRequest.getTransactionId());
        TransactionDTO trxDTO = syncTrxRequest2TransactionDtoMapper.apply(trxPreviewRequest);

        Mono<Boolean> trxChecks = checkInitiative(trxPreviewRequest, initiativeId)
                .flatMap(b -> checkOnboarded(trxPreviewRequest, trxDTO, initiativeId));

        return evaluate(trxChecks, trxDTO, initiativeId,
                x -> userInitiativeCountersRepository.findById(UserInitiativeCounters.buildId(trxDTO.getUserId(), initiativeId)),
                counters -> initiativesEvaluatorFacadeService.evaluateInitiativesBudgetAndRules(trxDTO, List.of(initiativeId), counters)
                        .map(Pair::getSecond));
    }

    private static final SynchronousTransactionResponseDTO EMPTY_RESPONSE = new SynchronousTransactionResponseDTO();
    @Override
    public Mono<SynchronousTransactionResponseDTO> authorizeTransaction(SynchronousTransactionRequestDTO trxAuthorizeRequest, String initiativeId) {
        log.trace("[SYNC_AUTHORIZE_TRANSACTION] Starting reward calculation for transaction {}", trxAuthorizeRequest.getTransactionId());
        TransactionDTO trxDTO = syncTrxRequest2TransactionDtoMapper.apply(trxAuthorizeRequest);

        Mono<Boolean> trxChecks = checkInitiative(trxAuthorizeRequest, initiativeId)
                .flatMap(initiativeFound -> checkSyncTrxAlreadyProcessed(trxAuthorizeRequest.getTransactionId(), initiativeId))
                .defaultIfEmpty(EMPTY_RESPONSE)
                .flatMap(x -> checkOnboarded(trxAuthorizeRequest, trxDTO, initiativeId));

        return lockCounterAndEvaluate(trxChecks, trxDTO, initiativeId);
    }

    private Mono<Boolean> checkInitiative(SynchronousTransactionRequestDTO request, String initiativeId) {
        return rewardContextHolderService.getInitiativeConfig(initiativeId)
                .map(i -> InitiativeRewardType.DISCOUNT.equals(i.getInitiativeRewardType()))
                .switchIfEmpty(Mono.just(false))
                .map(b -> checkingResult(b, request, initiativeId, RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND));
    }

    private Mono<Boolean> checkOnboarded(SynchronousTransactionRequestDTO request, TransactionDTO trx, String initiativeId) {
        return onboardedInitiativesService.isOnboarded(trx.getHpan(), trx.getTrxDate(), initiativeId)
                .map(b -> checkingResult(b, request, initiativeId, RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));
    }

    private Boolean checkingResult(Boolean b, SynchronousTransactionRequestDTO request, String initiativeId, String trxRejectionReasonNoInitiative) {
        if (b.equals(Boolean.TRUE)) {
            return Boolean.TRUE;
        } else {
            throw new TransactionSynchronousException(syncTrxRequest2TransactionDtoMapper.apply(request, initiativeId, List.of(trxRejectionReasonNoInitiative)));
        }
    }
}
