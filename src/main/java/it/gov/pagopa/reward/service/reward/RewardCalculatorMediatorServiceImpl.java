package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService{

    private final TransactionFilterService transactionFilterService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final UserInitiativeCountersUpdateService userInitiativeCountersUpdateService;
    private final InitiativesEvaluatorService initiativesEvaluatorService;

    public RewardCalculatorMediatorServiceImpl(TransactionFilterService transactionFilterService, OnboardedInitiativesService onboardedInitiativesService, UserInitiativeCountersRepository userInitiativeCountersRepository, UserInitiativeCountersUpdateService userInitiativeCountersUpdateService, InitiativesEvaluatorService initiativesEvaluatorService) {
        this.transactionFilterService = transactionFilterService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
        this.initiativesEvaluatorService = initiativesEvaluatorService;
    }

    @Override
    public Flux<RewardTransactionDTO> execute(Flux<TransactionDTO> transactionDTOFlux) {
        return transactionDTOFlux
                .filter(transactionFilterService::filter)
                .flatMap(this::retrieveInitiativesAndEvaluate);
    }

    private Mono<RewardTransactionDTO> retrieveInitiativesAndEvaluate(TransactionDTO trx) {
        return onboardedInitiativesService.getInitiatives(trx.getHpan(), trx.getTrxDate())
                .collectList()
                .flatMap(initiatives -> evaluate(trx, initiatives));
    }

    private Mono<RewardTransactionDTO> evaluate(TransactionDTO trx, List<String> initiatives) {
        String userId = trx.getHpan(); // TODO use userId
        return userInitiativeCountersRepository.findById(userId)
                .defaultIfEmpty(new UserInitiativeCounters(userId, new HashMap<>()))
                .mapNotNull(userCounters -> evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters))
                .flatMap(counters2rewardedTrx -> {
                    userInitiativeCountersUpdateService.update(counters2rewardedTrx.getFirst(), counters2rewardedTrx.getSecond());
                    return userInitiativeCountersRepository.save(counters2rewardedTrx.getFirst())
                            .then(Mono.just(counters2rewardedTrx.getSecond()));
                });
    }

    private Pair<UserInitiativeCounters, RewardTransactionDTO> evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters) {
        RewardTransactionDTO trxRewarded = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);
        if(trxRewarded!=null){
            return Pair.of(userCounters, trxRewarded);
        } else {
            return null;
        }
    }
}
