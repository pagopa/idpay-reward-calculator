package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.synchronous.op.recover.HandleSyncCounterUpdatingTrxService;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

abstract class BaseTrxSynchronousOp {

    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final HandleSyncCounterUpdatingTrxService handleSyncCounterUpdatingTrxService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;
    private final RewardTransaction2SynchronousTransactionResponseDTOMapper mapper;

    protected BaseTrxSynchronousOp(UserInitiativeCountersRepository userInitiativeCountersRepository, HandleSyncCounterUpdatingTrxService handleSyncCounterUpdatingTrxService, InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService, RewardTransaction2SynchronousTransactionResponseDTOMapper mapper) {
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.handleSyncCounterUpdatingTrxService = handleSyncCounterUpdatingTrxService;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.mapper = mapper;
    }

    protected Mono<SynchronousTransactionResponseDTO> lockCounterAndEvaluate(Mono<?> trxChecks, TransactionDTO trxDTO, String initiativeId) {
        return evaluate(trxChecks, trxDTO, initiativeId,
                x -> userInitiativeCountersRepository.findByIdThrottled(UserInitiativeCounters.buildId(trxDTO.getUserId(), initiativeId), trxDTO.getId())
                        .flatMap(counters -> handleSyncCounterUpdatingTrxService.checkUpdatingTrx(trxDTO, counters)),
                counters -> initiativesEvaluatorFacadeService.evaluateAndUpdateBudget(
                        trxDTO,
                        List.of(initiativeId),
                        counters)
        );
    }

    protected <T> Mono<SynchronousTransactionResponseDTO> evaluate(Mono<T> trxChecks, TransactionDTO trxDTO, String initiativeId,
                                                                   Function<T, Mono<UserInitiativeCounters>> counterRetriever,
                                                                   Function<UserInitiativeCountersWrapper, Mono<RewardTransactionDTO>> evaluator) {
        return trxChecks
                .flatMap(counterRetriever)
                .switchIfEmpty(Mono.just(new UserInitiativeCounters(trxDTO.getUserId(), initiativeId)))
                .flatMap(userInitiativeCounters -> evaluator.apply(new UserInitiativeCountersWrapper(trxDTO.getUserId(), new HashMap<>(Map.of(initiativeId, userInitiativeCounters)))))
                .map(rewardTransaction -> mapper.apply(trxDTO.getId(), initiativeId, rewardTransaction));
    }

}
