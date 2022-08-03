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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService{

    private final TransactionFilterService transactionFilterService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final RuleEngineService ruleEngineService;
    private final UserInitiativeCountersUpdateService userInitiativeCountersUpdateService;

    public RewardCalculatorMediatorServiceImpl(TransactionFilterService transactionFilterService, OnboardedInitiativesService onboardedInitiativesService, UserInitiativeCountersRepository userInitiativeCountersRepository, RuleEngineService ruleEngineService, UserInitiativeCountersUpdateService userInitiativeCountersUpdateService) {
        this.transactionFilterService = transactionFilterService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.ruleEngineService = ruleEngineService;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
    }

    @Override
    public Flux<RewardTransactionDTO> execute(Flux<TransactionDTO> transactionDTOFlux) {
        return transactionDTOFlux
                .filter(transactionFilterService::filter)
                .flatMap(this::evaluate);
    }

    private Mono<RewardTransactionDTO> evaluate(TransactionDTO trx) {
        return onboardedInitiativesService.getInitiatives(trx.getHpan(), trx.getTrxDate())
                .collectList()
                .flatMap(initiatives -> evaluate(trx, initiatives));
    }

    private Mono<RewardTransactionDTO> evaluate(TransactionDTO trx, List<String> initiatives) {
        String userId = trx.getHpan(); // TODO use userId
        return userInitiativeCountersRepository.findById(userId)
                .defaultIfEmpty(new UserInitiativeCounters(userId, new HashMap<>()))
                .mapNotNull(userCounters -> pairCounters2TrxRewarded(trx, initiatives, userCounters))
                .flatMap(counters2rewardedTrx -> {
                    userInitiativeCountersUpdateService.update(counters2rewardedTrx.getFirst(), counters2rewardedTrx.getSecond());
                    return userInitiativeCountersRepository.save(counters2rewardedTrx.getFirst())
                            .then(Mono.just(counters2rewardedTrx.getSecond()));
                });
    }

    private Pair<UserInitiativeCounters, RewardTransactionDTO> pairCounters2TrxRewarded(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters) {
        List<String> notExhaustedInitiatives = new ArrayList<>();
        List<String> rejectedInitiativesForBudget = new ArrayList<>();
        initiatives.forEach(initiativeId -> {
            if(userCounters.getInitiatives().get(initiativeId).isExhaustedBudget()) {
                rejectedInitiativesForBudget.add("BUDGET_EXHAUSTED_for_initiativeId_%s".formatted(initiativeId));
            } else {
                notExhaustedInitiatives.add(initiativeId);
            }
        });
        RewardTransactionDTO trxRewarded = ruleEngineService.applyRules(trx, notExhaustedInitiatives, userCounters);
        if(trxRewarded!=null){
            trxRewarded.getRejectionReasons().addAll(rejectedInitiativesForBudget);
            return Pair.of(userCounters, trxRewarded);
        } else {
            return null;
        }
    }
}
