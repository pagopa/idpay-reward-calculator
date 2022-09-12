package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.utils.RewardConstants;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService {

    private final OperationTypeHandlerService operationTypeHandlerService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final InitiativesEvaluatorService initiativesEvaluatorService;
    private final UserInitiativeCountersUpdateService userInitiativeCountersUpdateService;
    private final Transaction2RewardTransactionMapper rewardTransactionMapper;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    public RewardCalculatorMediatorServiceImpl(OperationTypeHandlerService operationTypeHandlerService, OnboardedInitiativesService onboardedInitiativesService, UserInitiativeCountersRepository userInitiativeCountersRepository, InitiativesEvaluatorService initiativesEvaluatorService, UserInitiativeCountersUpdateService userInitiativeCountersUpdateService, Transaction2RewardTransactionMapper rewardTransactionMapper, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.operationTypeHandlerService = operationTypeHandlerService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.initiativesEvaluatorService = initiativesEvaluatorService;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
        this.rewardTransactionMapper = rewardTransactionMapper;
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
                .flatMap(operationTypeHandlerService::handleOperationType)
                .flatMap(this::retrieveInitiativesAndEvaluate)

                .onErrorResume(e -> {
                    errorNotifierService.notifyTransactionEvaluation(message, "An error occurred evaluating transaction", true, e);
                    return Mono.empty();
                })
                .doFinally(x -> log.info("[PERFORMANCE_LOG] [REWARD] - Time between before and after evaluate message %d ms with payload: %s".formatted(System.currentTimeMillis()-startTime,message.getPayload())));
    }

    private TransactionDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyTransactionEvaluation(message, "Unexpected JSON", true, e));
    }

    private Mono<RewardTransactionDTO> retrieveInitiativesAndEvaluate(TransactionDTO trx) {
        if(CollectionUtils.isEmpty(trx.getRejectionReasons())){
            Mono<List<String>> initiatives2Evaluate = buildInitiativeListMono(trx);
            if(initiatives2Evaluate!=null) {
                // in case of complete reverse, we will yet call evaluate method in order to update counters
                return initiatives2Evaluate.flatMap(initiatives -> evaluate(trx, initiatives));
            } else {
                trx.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));
                return Mono.just(rewardTransactionMapper.apply(trx));
            }
        } else {
            log.trace("[REWARD] [REWARD_KO] Transaction discarded: {}", trx.getRejectionReasons());
            return Mono.just(rewardTransactionMapper.apply(trx));
        }
    }

    /** In case of complete reverse, it will retrieve just the rewarded initiatives, otherwise it will search for the active initiatives */
    private Mono<List<String>> buildInitiativeListMono(TransactionDTO trx) {
        if(OperationType.CHARGE.equals(trx.getOperationTypeTranscoded()) || BigDecimal.ZERO.compareTo(trx.getEffectiveAmount()) < 0){
            return onboardedInitiativesService.getInitiatives(trx.getHpan(), trx.getTrxChargeDate())
                    .collectList();
        } else {
            if(trx.getReversalInfo() != null){
                return Mono.just(new ArrayList<>(trx.getReversalInfo().getPreviousRewards().keySet()));
            } else {
                log.trace("[REWARD] [REWARD_KO] Recognized REVERSAL operation without previous rewards");
                return null;
            }
        }
    }

    private Mono<RewardTransactionDTO> evaluate(TransactionDTO trx, List<String> initiatives) {
        String userId = trx.getUserId();
        return userInitiativeCountersRepository.findById(userId)
                .defaultIfEmpty(new UserInitiativeCounters(userId, new HashMap<>()))
                .map(userCounters -> evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters))
                .flatMap(counters2rewardedTrx -> {
                    userInitiativeCountersUpdateService.update(counters2rewardedTrx.getFirst(), counters2rewardedTrx.getSecond());
                    return userInitiativeCountersRepository.save(counters2rewardedTrx.getFirst())
                            .then(Mono.just(counters2rewardedTrx.getSecond()));
                });
    }

    private Pair<UserInitiativeCounters, RewardTransactionDTO> evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters) {
        RewardTransactionDTO trxRewarded = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);
        return Pair.of(userCounters, trxRewarded);
    }
}
