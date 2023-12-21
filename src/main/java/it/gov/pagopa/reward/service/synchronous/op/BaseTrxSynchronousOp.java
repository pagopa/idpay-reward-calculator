package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.TransactionProcessed2SyncTrxResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.exception.custom.TransactionAlreadyProcessedException;
import it.gov.pagopa.reward.exception.custom.TransactionAssignedToAnotherUserException;
import it.gov.pagopa.reward.model.BaseOnboardingInfo;
import it.gov.pagopa.reward.model.OnboardingInfo;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.synchronous.op.recover.HandleSyncCounterUpdatingTrxService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
abstract class BaseTrxSynchronousOp {

    private final TransactionProcessedRepository transactionProcessedRepository;
    private final TransactionProcessed2SyncTrxResponseDTOMapper syncTrxResponseDTOMapper;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final HandleSyncCounterUpdatingTrxService handleSyncCounterUpdatingTrxService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;
    private final RewardTransaction2SynchronousTransactionResponseDTOMapper mapper;
    private final RewardContextHolderService rewardContextHolderService;

    protected BaseTrxSynchronousOp(
            TransactionProcessedRepository transactionProcessedRepository,
            TransactionProcessed2SyncTrxResponseDTOMapper syncTrxResponseDTOMapper, UserInitiativeCountersRepository userInitiativeCountersRepository,
            HandleSyncCounterUpdatingTrxService handleSyncCounterUpdatingTrxService,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            RewardTransaction2SynchronousTransactionResponseDTOMapper mapper, RewardContextHolderService rewardContextHolderService) {
        this.transactionProcessedRepository = transactionProcessedRepository;
        this.syncTrxResponseDTOMapper = syncTrxResponseDTOMapper;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.handleSyncCounterUpdatingTrxService = handleSyncCounterUpdatingTrxService;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.mapper = mapper;
        this.rewardContextHolderService = rewardContextHolderService;
    }

    protected Mono<SynchronousTransactionResponseDTO> checkSyncTrxAlreadyProcessed(String trxId, String userId, String initiativeId) {
        return transactionProcessedRepository.findById(trxId)
                .flatMap(trxProcessed -> {
                            if(userId!=null && !trxProcessed.getUserId().equals(userId)){
                                return Mono.error(new TransactionAssignedToAnotherUserException(RewardConstants.ExceptionMessage.TRANSACTION_ASSIGNED_TO_ANOTHER_USER.formatted(userId, trxId)));
                            }
                            return retrieveInitiative2OnboardingInfo(trxProcessed.getRewards().get(initiativeId))
                                    .flatMap(i2o -> userInitiativeCountersRepository.findById(UserInitiativeCounters.buildId(retrieveCounterEntityId(i2o.getFirst(), i2o.getSecond(), trxProcessed.getUserId()), initiativeId))
                                            .mapNotNull(ctr -> {
                                                // if the requested trx is not stuck, thus it has already been processed
                                                if (CollectionUtils.isEmpty(ctr.getUpdatingTrxId()) || !ctr.getUpdatingTrxId().contains(trxId)) {
                                                    throw new TransactionAlreadyProcessedException(syncTrxResponseDTOMapper.apply(trxProcessed, initiativeId));
                                                } else {
                                                    log.info("[SYNC_TRANSACTION] Actual trx was stuck! recovering it: trxId:{} counterId:{}", trxId, ctr.getId());
                                                    return null;
                                                }
                                            }));
                        }
                );
    }

    protected Mono<SynchronousTransactionResponseDTO> lockCounterAndEvaluate(Mono<Pair<InitiativeConfig, OnboardingInfo>> trxChecks, TransactionDTO trxDTO, String initiativeId) {
        return evaluate(trxChecks, trxDTO, initiativeId,
                i2o -> userInitiativeCountersRepository.findByIdThrottled(UserInitiativeCounters.buildId(retrieveCounterEntityId(i2o.getFirst(),i2o.getSecond(),trxDTO), initiativeId), trxDTO.getId())
                        .flatMap(counters -> handleSyncCounterUpdatingTrxService.checkUpdatingTrx(trxDTO, counters)),
                counters -> initiativesEvaluatorFacadeService.evaluateAndUpdateBudget(
                        trxDTO,
                        List.of(initiativeId),
                        counters)
        );
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

    protected Mono<Pair<InitiativeConfig, OnboardingInfo>> retrieveInitiative2OnboardingInfo(Reward reward) {
        return rewardContextHolderService.getInitiativeConfig(reward.getInitiativeId())
                .map(i -> Pair.of(i,new BaseOnboardingInfo(reward.getInitiativeId(),reward.getFamilyId())));
    }

}
