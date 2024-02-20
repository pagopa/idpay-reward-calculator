package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.exception.custom.InitiativeNotActiveException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotFoundOrNotDiscountException;
import it.gov.pagopa.reward.exception.custom.InitiativeNotInContainerException;
import it.gov.pagopa.reward.exception.custom.PendingCounterException;
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
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
abstract class BaseTrxSynchronousOp {

    protected final UserInitiativeCountersRepository userInitiativeCountersRepository;
    protected final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;
    protected final RewardTransaction2SynchronousTransactionResponseDTOMapper mapper;
    protected final RewardContextHolderService rewardContextHolderService;
    protected final Transaction2RewardTransactionMapper rewardTransactionMapper;
    protected final UserInitiativeCountersUpdateService userInitiativeCountersUpdateService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    protected final SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper;


    protected BaseTrxSynchronousOp(
            UserInitiativeCountersRepository userInitiativeCountersRepository,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            RewardTransaction2SynchronousTransactionResponseDTOMapper mapper,
            RewardContextHolderService rewardContextHolderService,
            Transaction2RewardTransactionMapper rewardTransactionMapper,
            UserInitiativeCountersUpdateService userInitiativeCountersUpdateService,
            OnboardedInitiativesService onboardedInitiativesService,
            SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper syncTrxRequest2TransactionDtoMapper) {
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.mapper = mapper;
        this.rewardContextHolderService = rewardContextHolderService;
        this.rewardTransactionMapper = rewardTransactionMapper;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.syncTrxRequest2TransactionDtoMapper = syncTrxRequest2TransactionDtoMapper;
    }

    protected Mono<SynchronousTransactionResponseDTO> evaluate(Mono<Pair<InitiativeConfig, OnboardingInfo>> trxChecks, TransactionDTO trxDTO, String initiativeId,
                                                               Function<Pair<InitiativeConfig, OnboardingInfo>, Mono<UserInitiativeCounters>> counterRetriever,
                                                               Function<UserInitiativeCountersWrapper, Mono<RewardTransactionDTO>> evaluator) {
        return trxChecks
                .flatMap(i2o -> counterRetriever.apply(i2o)
                        .defaultIfEmpty(new UserInitiativeCounters(retrieveCounterEntityId(i2o.getFirst(),i2o.getSecond(),trxDTO), i2o.getFirst().getBeneficiaryType() ,initiativeId))
                )
                .flatMap(userInitiativeCounters -> evaluator.apply(new UserInitiativeCountersWrapper(userInitiativeCounters.getEntityId(), new HashMap<>(Map.of(initiativeId, userInitiativeCounters)))))
                .map(rewardTransaction -> mapper.apply(trxDTO.getId(), initiativeId, rewardTransaction));
    }

    protected String retrieveCounterEntityId(InitiativeConfig initiativeConfig, OnboardingInfo onboardingInfo, TransactionDTO trxDTO) {
        return retrieveCounterEntityId(initiativeConfig, onboardingInfo, trxDTO.getUserId());
    }

    private   String retrieveCounterEntityId(InitiativeConfig initiativeConfig, OnboardingInfo onboardingInfo, String userId) {
        return InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(initiativeConfig.getBeneficiaryType())
                ? onboardingInfo.getFamilyId() : userId;
    }

    protected Mono<RewardTransactionDTO> handlePendingCounter(String flowName, TransactionDTO trxDTO, String initiativeId, UserInitiativeCounters counter, long rewardCents) {
        if(!counter.getPendingTrx().getId().equals(trxDTO.getId()) ||
                !trxDTO.getOperationTypeTranscoded().equals(counter.getPendingTrx().getOperationTypeTranscoded())){
            log.info("[{}] Cannot process transaction {} of userId {} on initiative {}: another transaction ({} {}) is locking the counter {}",
                    flowName, trxDTO.getId(), trxDTO.getUserId(), initiativeId, counter.getPendingTrx().getOperationTypeTranscoded(), counter.getPendingTrx().getId(), counter.getId());
            return Mono.error(new PendingCounterException());
        } else {
            log.info("[{}] processing an already pending transaction: {} of userId {} on initiative {} on counter {}",
                    flowName, trxDTO.getId(), trxDTO.getUserId(), initiativeId, counter.getId());
            return rewardContextHolderService.getInitiativeConfig(initiativeId)
                    .map(i -> buildRewardedTrxHavingRewardCents(trxDTO, initiativeId, rewardCents, i));
        }
    }

    protected Mono<Pair<UserInitiativeCountersWrapper, RewardTransactionDTO>> handleUnlockedCounter(String flowName, TransactionDTO trxDTO, String initiativeId, UserInitiativeCountersWrapper counters, long rewardCents) {
        log.info("[{}] processing transaction {} of userId {} on initiative {} on an unlocked counter {} matching the requested version",
                flowName, trxDTO.getId(), trxDTO.getUserId(), initiativeId, counters.getInitiatives().get(initiativeId).getId());
        return rewardContextHolderService.getInitiativeConfig(initiativeId)
                .flatMap(i -> {
                    RewardTransactionDTO rewardedTrx = buildRewardedTrxHavingRewardCents(trxDTO, initiativeId, rewardCents, i);
                    return userInitiativeCountersUpdateService.update(counters, rewardedTrx)
                            .map(r -> Pair.of(counters, r));
                });
    }

    protected RewardTransactionDTO buildRewardedTrxHavingRewardCents(TransactionDTO trxDTO, String initiativeId, long rewardCents, InitiativeConfig i) {
        RewardTransactionDTO rewardedTrx = rewardTransactionMapper.apply(trxDTO);
        rewardedTrx.setInitiatives(List.of(initiativeId));
        rewardedTrx.getRewards().put(initiativeId, new Reward(initiativeId, i.getOrganizationId(), CommonUtilities.centsToEuro(rewardCents)));
        return rewardedTrx;
    }

    protected Mono<RewardTransactionDTO> lockCounter(Mono<Pair<UserInitiativeCountersWrapper, RewardTransactionDTO>> ctr2rewardMono, TransactionDTO trxDTO, UserInitiativeCounters counter) {
        return ctr2rewardMono
                .flatMap(ctr2reward -> {
                            counter.setPendingTrx(trxDTO);
                            return userInitiativeCountersRepository.save(counter)
                                    .map(x -> ctr2reward.getSecond());
                        }
                );
    }

    protected Mono<UserInitiativeCounters> findUserInitiativeCounter(String initiativeId, Pair<InitiativeConfig, OnboardingInfo> i2o, TransactionDTO trxDTO) {
        return userInitiativeCountersRepository.findById(UserInitiativeCounters.buildId(retrieveCounterEntityId(i2o.getFirst(), i2o.getSecond(), trxDTO), initiativeId));
    }

    protected Mono<Boolean> checkInitiative(SynchronousTransactionRequestDTO request, String initiativeId) {
        return rewardContextHolderService.getInitiativeConfig(initiativeId)
                .map(i -> InitiativeRewardType.DISCOUNT.equals(i.getInitiativeRewardType()))
                .switchIfEmpty(Mono.just(false))
                .map(b -> checkingResult(b, request, initiativeId, RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND))
                .map(b -> checkingResult(rewardContextHolderService.getRewardRulesKieInitiativeIds().contains(initiativeId), request, initiativeId, RewardConstants.TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY));
    }

    protected Mono<Pair<InitiativeConfig, OnboardingInfo>> checkOnboarded(SynchronousTransactionRequestDTO request, TransactionDTO trx, String initiativeId) {
        return onboardedInitiativesService.isOnboarded(trx.getHpan(), trx.getTrxChargeDate(), initiativeId)
                .switchIfEmpty(Mono.error(new InitiativeNotActiveException(String.format(RewardConstants.ExceptionMessage.INITIATIVE_NOT_ACTIVE_FOR_USER_MSG,initiativeId),syncTrxRequest2TransactionDtoMapper
                        .apply(request, initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE)))));
    }

    protected Boolean checkingResult(Boolean b, SynchronousTransactionRequestDTO request, String initiativeId, String trxRejectionReasonNoInitiative) {
        if (b.equals(Boolean.TRUE)) {
            return Boolean.TRUE;
        } else {
            if (RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND.equalsIgnoreCase(trxRejectionReasonNoInitiative)) {
                throw new InitiativeNotFoundOrNotDiscountException(String.format(RewardConstants.ExceptionMessage.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT_MSG, initiativeId), syncTrxRequest2TransactionDtoMapper
                        .apply(request, initiativeId, List.of(trxRejectionReasonNoInitiative)));
            } else {
                throw new InitiativeNotInContainerException(String.format(RewardConstants.ExceptionMessage.INITIATIVE_NOT_READY_MSG, initiativeId), syncTrxRequest2TransactionDtoMapper
                        .apply(request, initiativeId, List.of(trxRejectionReasonNoInitiative)));
            }
        }
    }

}
