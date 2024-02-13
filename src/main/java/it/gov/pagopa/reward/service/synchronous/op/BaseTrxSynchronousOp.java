package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.exception.custom.PendingCounterException;
import it.gov.pagopa.reward.model.OnboardingInfo;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateService;
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

    protected BaseTrxSynchronousOp(
            UserInitiativeCountersRepository userInitiativeCountersRepository,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            RewardTransaction2SynchronousTransactionResponseDTOMapper mapper,
            RewardContextHolderService rewardContextHolderService,
            Transaction2RewardTransactionMapper rewardTransactionMapper,
            UserInitiativeCountersUpdateService userInitiativeCountersUpdateService) {
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.mapper = mapper;
        this.rewardContextHolderService = rewardContextHolderService;
        this.rewardTransactionMapper = rewardTransactionMapper;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
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

    private RewardTransactionDTO buildRewardedTrxHavingRewardCents(TransactionDTO trxDTO, String initiativeId, long rewardCents, InitiativeConfig i) {
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

}
