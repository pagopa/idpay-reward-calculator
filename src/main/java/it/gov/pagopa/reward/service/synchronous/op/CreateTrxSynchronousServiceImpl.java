package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.TransactionProcessed2SyncTrxResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotFoundOrNotDiscountException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotInContainerException;
import it.gov.pagopa.reward.model.OnboardingInfo;
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

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionMessage;

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
        super(transactionProcessedRepository, syncTrxResponseDTOMapper, userInitiativeCountersRepository, handleSyncCounterUpdatingTrxService, initiativesEvaluatorFacadeService, rewardTransaction2SynchronousTransactionResponseDTOMapper, rewardContextHolderService);

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

        Mono<Pair<InitiativeConfig,OnboardingInfo>> trxChecks = checkInitiative(trxPreviewRequest, initiativeId)
                .flatMap(b -> checkOnboarded(trxPreviewRequest, trxDTO, initiativeId));

        return evaluate(trxChecks, trxDTO, initiativeId,
                i2o -> userInitiativeCountersRepository.findById(UserInitiativeCounters.buildId(retrieveCounterEntityId(i2o.getFirst(),i2o.getSecond(),trxDTO), initiativeId)),
                counters -> initiativesEvaluatorFacadeService.evaluateInitiativesBudgetAndRules(trxDTO, List.of(initiativeId), counters)
                        .map(Pair::getSecond));
    }

    private static final SynchronousTransactionResponseDTO EMPTY_RESPONSE = new SynchronousTransactionResponseDTO();
    @Override
    public Mono<SynchronousTransactionResponseDTO> authorizeTransaction(SynchronousTransactionRequestDTO trxAuthorizeRequest, String initiativeId) {
        log.trace("[SYNC_AUTHORIZE_TRANSACTION] Starting reward calculation for transaction {}", trxAuthorizeRequest.getTransactionId());
        TransactionDTO trxDTO = syncTrxRequest2TransactionDtoMapper.apply(trxAuthorizeRequest);

        Mono<Pair<InitiativeConfig,OnboardingInfo>> trxChecks = checkInitiative(trxAuthorizeRequest, initiativeId)
                .flatMap(initiativeFound -> checkSyncTrxAlreadyProcessed(trxAuthorizeRequest.getTransactionId(), initiativeId))
                .defaultIfEmpty(EMPTY_RESPONSE)
                .flatMap(x -> checkOnboarded(trxAuthorizeRequest, trxDTO, initiativeId));

        return lockCounterAndEvaluate(trxChecks, trxDTO, initiativeId);
    }

    private Mono<Boolean> checkInitiative(SynchronousTransactionRequestDTO request, String initiativeId) {
        return rewardContextHolderService.getInitiativeConfig(initiativeId)
                .map(i -> InitiativeRewardType.DISCOUNT.equals(i.getInitiativeRewardType()))
                .switchIfEmpty(Mono.just(false))
                .map(b -> checkingResult(b, request, initiativeId, RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND))
                .map(b -> checkingResult(rewardContextHolderService.getRewardRulesKieInitiativeIds().contains(initiativeId), request, initiativeId, RewardConstants.TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY));
    }

    private Mono<Pair<InitiativeConfig, OnboardingInfo>> checkOnboarded(SynchronousTransactionRequestDTO request, TransactionDTO trx, String initiativeId) {
        return onboardedInitiativesService.isOnboarded(trx.getHpan(), trx.getTrxChargeDate(), initiativeId)
                .switchIfEmpty(Mono.error(new InitiativeNotActiveException(String.format(ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,initiativeId),syncTrxRequest2TransactionDtoMapper
                        .apply(request, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE)))));
    }

    private Boolean checkingResult(Boolean b, SynchronousTransactionRequestDTO request, String initiativeId, String trxRejectionReasonNoInitiative) {
        if (b.equals(Boolean.TRUE)) {
            return Boolean.TRUE;
        } else {
            if (RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND.equalsIgnoreCase(trxRejectionReasonNoInitiative)) {
                throw new InitiativeNotFoundOrNotDiscountException(String.format(ExceptionMessage.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT_MSG, initiativeId), syncTrxRequest2TransactionDtoMapper
                        .apply(request, initiativeId, List.of(trxRejectionReasonNoInitiative)));
            } else {
                throw new InitiativeNotInContainerException(String.format(ExceptionMessage.INITIATIVE_NOT_READY_MSG, initiativeId), syncTrxRequest2TransactionDtoMapper
                        .apply(request, initiativeId, List.of(trxRejectionReasonNoInitiative)));
            }
        }

    }

    }