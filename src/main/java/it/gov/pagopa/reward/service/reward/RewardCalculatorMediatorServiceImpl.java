package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService {

    private final OnboardedInitiativesService onboardedInitiativesService;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final InitiativesEvaluatorService initiativesEvaluatorService;
    private final UserInitiativeCountersUpdateService userInitiativeCountersUpdateService;
    private final TransactionProcessedService transactionProcessedService;
    private final ErrorNotifierService errorNotifierService;
    private final ObjectReader objectReader;

    public RewardCalculatorMediatorServiceImpl(OnboardedInitiativesService onboardedInitiativesService, UserInitiativeCountersRepository userInitiativeCountersRepository, InitiativesEvaluatorService initiativesEvaluatorService, UserInitiativeCountersUpdateService userInitiativeCountersUpdateService, TransactionProcessedService transactionProcessedService, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.initiativesEvaluatorService = initiativesEvaluatorService;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
        this.transactionProcessedService = transactionProcessedService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(TransactionDTO.class);
    }

    @Override
    public Flux<RewardTransactionDTO> execute(Flux<Message<String>> messageFlux) {
        return messageFlux.flatMap(this::execute);
    }

    public Mono<RewardTransactionDTO> execute(Message<String> message) {

        long startTime = System.currentTimeMillis();
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMap(this::checkDuplicateTransaction)
                .flatMap(this::retrieveInitiativesAndEvaluate)

                .onErrorResume(e -> {
                    errorNotifierService.notifyTransactionEvaluation(message, "An error occurred evaluating transaction", true, e);
                    return Mono.empty();
                })
                .doFinally(x -> log.info("[PERFORMANCE_LOG] - Time between before and after evaluate message %d ms with payload: %s".formatted(System.currentTimeMillis()-startTime,message.getPayload())));
    }

    private TransactionDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyTransactionEvaluation(message, "Unexpected JSON", true, e));
    }

    private Mono<TransactionDTO> checkDuplicateTransaction(TransactionDTO trx) {
        return transactionProcessedService.getProcessedTransactions(transactionProcessedService.computeTrxId(trx))
                .flatMap(result -> {
                        log.info("[DUPLICATE_TRX] Already processed transaction {}", result);
                        return Mono.<TransactionDTO>error(new IllegalStateException("[DUPLICATE_TRX] Already processed transaction"));
                })
                .defaultIfEmpty(trx)
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<RewardTransactionDTO> retrieveInitiativesAndEvaluate(TransactionDTO trx) {
        return onboardedInitiativesService.getInitiatives(trx.getHpan(), trx.getTrxDate())
                .collectList()
                .flatMap(initiatives -> evaluate(trx, initiatives));
    }

    private Mono<RewardTransactionDTO> evaluate(TransactionDTO trx, List<String> initiatives) {
        String userId = trx.getUserId();
        return userInitiativeCountersRepository.findById(userId)
                .defaultIfEmpty(new UserInitiativeCounters(userId, new HashMap<>()))
                .mapNotNull(userCounters -> evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters))
                .flatMap(counters2rewardedTrx -> {
                    RewardTransactionDTO rewardedTrx = counters2rewardedTrx.getSecond();
                    userInitiativeCountersUpdateService.update(counters2rewardedTrx.getFirst(), rewardedTrx);

                    return transactionProcessedService.save(rewardedTrx)
                            .then(userInitiativeCountersRepository.save(counters2rewardedTrx.getFirst()))
                            .then(Mono.just(rewardedTrx));
                });
    }

    private Pair<UserInitiativeCounters, RewardTransactionDTO> evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters) {
        RewardTransactionDTO trxRewarded = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);
        if (trxRewarded != null) {
            return Pair.of(userCounters, trxRewarded);
        } else {
            return null;
        }
    }
}
