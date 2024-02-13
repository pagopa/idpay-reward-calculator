package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionAuthRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotFoundOrNotDiscountException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotInContainerException;
import it.gov.pagopa.reward.exception.custom.InvalidCounterVersionException;
import it.gov.pagopa.reward.model.OnboardingInfo;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateService;
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

    private final OnboardedInitiativesService onboardedInitiativesService;
    private final SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public CreateTrxSynchronousServiceImpl(
            RewardContextHolderService rewardContextHolderService,
            OnboardedInitiativesService onboardedInitiativesService,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            UserInitiativeCountersRepository userInitiativeCountersRepository,
            SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper,
            RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper,
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

        this.onboardedInitiativesService = onboardedInitiativesService;
        this.syncTrxRequest2TransactionDtoMapper = syncTrxRequest2TransactionDtoMapper;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId) {
        log.trace("[SYNC_PREVIEW_TRANSACTION] Starting reward preview calculation for transaction {}", trxPreviewRequest.getTransactionId());
        TransactionDTO trxDTO = syncTrxRequest2TransactionDtoMapper.apply(trxPreviewRequest);

        Mono<Pair<InitiativeConfig,OnboardingInfo>> trxChecks = checkInitiative(trxPreviewRequest, initiativeId)
                .flatMap(b -> checkOnboarded(trxPreviewRequest, trxDTO, initiativeId));

        return evaluate(trxChecks, trxDTO, initiativeId,
                i2o -> findUserInitiativeCounter(initiativeId, i2o, trxDTO),
                counters -> initiativesEvaluatorFacadeService.evaluateInitiativesBudgetAndRules(trxDTO, List.of(initiativeId), counters)
                        .map(Pair::getSecond));
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> authorizeTransaction(SynchronousTransactionAuthRequestDTO trxAuthorizeRequest, String initiativeId, long counterVersion) {
        log.trace("[SYNC_AUTHORIZE_TRANSACTION] Starting authorization for transaction {} having reward {} on counterVersion {}", trxAuthorizeRequest.getTransactionId(), trxAuthorizeRequest.getRewardCents(), counterVersion);
        TransactionDTO trxDTO = syncTrxRequest2TransactionDtoMapper.apply(trxAuthorizeRequest);

        Mono<Pair<InitiativeConfig,OnboardingInfo>> trxChecks = checkInitiative(trxAuthorizeRequest, initiativeId)
                .flatMap(x -> checkOnboarded(trxAuthorizeRequest, trxDTO, initiativeId));

        return evaluate(trxChecks, trxDTO, initiativeId,
                i2o -> findUserInitiativeCounter(initiativeId, i2o, trxDTO),
                counters -> checkCounterOrEvaluateThenUpdate(trxDTO, initiativeId, counters, counterVersion, trxAuthorizeRequest.getRewardCents())
        );
    }

    private Mono<UserInitiativeCounters> findUserInitiativeCounter(String initiativeId, Pair<InitiativeConfig, OnboardingInfo> i2o, TransactionDTO trxDTO) {
        return userInitiativeCountersRepository.findById(UserInitiativeCounters.buildId(retrieveCounterEntityId(i2o.getFirst(), i2o.getSecond(), trxDTO), initiativeId));
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

    private Mono<RewardTransactionDTO> checkCounterOrEvaluateThenUpdate(TransactionDTO trxDTO, String initiativeId, UserInitiativeCountersWrapper counters, long counterVersion, long rewardCents) {
        UserInitiativeCounters counter = counters.getInitiatives().get(initiativeId);

        Mono<Pair<UserInitiativeCountersWrapper, RewardTransactionDTO>> ctr2rewardMono;
        if(counter.getPendingTrx()!=null){
            return handlePendingCounter("SYNC_AUTHORIZE_TRANSACTION", trxDTO, initiativeId, counter, rewardCents);
        } else if(counterVersion != counter.getVersion()+1){
            log.info("[SYNC_AUTHORIZE_TRANSACTION] counterVersion provided by authorization of transaction {} of userId {} on initiative {} doesn't meet counter {}: requested {}, actual {}", trxDTO.getId(), trxDTO.getUserId(), initiativeId, counter.getId(), counterVersion, counter.getVersion());
            ctr2rewardMono = handleAuthorizationCounterVersionMismatch(trxDTO, initiativeId, counters, rewardCents, counter, counterVersion);
        } else {
            ctr2rewardMono = handleUnlockedCounter("SYNC_AUTHORIZE_TRANSACTION", trxDTO, initiativeId, counters, rewardCents);
        }

        return lockCounter(ctr2rewardMono, trxDTO, counter);
    }

    private Mono<Pair<UserInitiativeCountersWrapper, RewardTransactionDTO>> handleAuthorizationCounterVersionMismatch(TransactionDTO trxDTO, String initiativeId, UserInitiativeCountersWrapper counters, long rewardCents, UserInitiativeCounters counter, long counterVersion) {
        return initiativesEvaluatorFacadeService.evaluateInitiativesBudgetAndRules(trxDTO, List.of(initiativeId), counters)
                .doOnNext(ctr2reward -> {
                    if(ctr2reward.getSecond().getRewards().get(initiativeId)==null ||
                            !CommonUtilities.euroToCents(ctr2reward.getSecond().getRewards().get(initiativeId).getAccruedReward())
                            .equals(rewardCents)){
                        log.info("[SYNC_AUTHORIZE_TRANSACTION] Cannot authorize transaction {} of userId {} on initiative {}: counter ({}) version mismatch ({} actual {}) and reward is not more valid (requested {} actual {})",
                                trxDTO.getId(), trxDTO.getUserId(), initiativeId,
                                counter.getId(),
                                counter.getVersion(),
                                counterVersion,
                                rewardCents,
                                ctr2reward.getSecond().getRewards().get(initiativeId));
                        throw new InvalidCounterVersionException();
                    }
                });
    }

}