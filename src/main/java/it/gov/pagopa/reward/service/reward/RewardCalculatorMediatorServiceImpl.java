package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService {

    private final OperationTypeHandlerService operationTypeHandlerService;
    private final TransactionValidatorService transactionValidatorService;
    private final TransactionProcessedService transactionProcessedService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final InitiativesEvaluatorService initiativesEvaluatorService;
    private final UserInitiativeCountersUpdateService userInitiativeCountersUpdateService;
    private final Transaction2RewardTransactionMapper rewardTransactionMapper;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardCalculatorMediatorServiceImpl(OperationTypeHandlerService operationTypeHandlerService, TransactionValidatorService transactionValidatorService, TransactionProcessedService transactionProcessedService, OnboardedInitiativesService onboardedInitiativesService, UserInitiativeCountersRepository userInitiativeCountersRepository, InitiativesEvaluatorService initiativesEvaluatorService, UserInitiativeCountersUpdateService userInitiativeCountersUpdateService, Transaction2RewardTransactionMapper rewardTransactionMapper, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.operationTypeHandlerService = operationTypeHandlerService;
        this.transactionValidatorService = transactionValidatorService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.initiativesEvaluatorService = initiativesEvaluatorService;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
        this.rewardTransactionMapper = rewardTransactionMapper;
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
                .flatMap(transactionProcessedService::checkDuplicateTransactions)
                .flatMap(operationTypeHandlerService::handleOperationType)
                .map(transactionValidatorService::validate)
                .flatMap(this::retrieveInitiativesAndEvaluate)

                .onErrorResume(e -> {
                    errorNotifierService.notifyTransactionEvaluation(message, "An error occurred evaluating transaction", true, e);
                    return Mono.empty();
                })
                .doFinally(x -> log.info("[PERFORMANCE_LOG] [REWARD] - Time between before and after evaluate message {} ms with payload: {}", System.currentTimeMillis() - startTime, message.getPayload()));
    }

    private TransactionDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyTransactionEvaluation(message, "Unexpected JSON", true, e));
    }

    private Mono<RewardTransactionDTO> retrieveInitiativesAndEvaluate(TransactionDTO trx) {
        if (CollectionUtils.isEmpty(trx.getRejectionReasons())) {
            return onboardedInitiativesService.getInitiatives(trx)
                    .collectList()
                    .flatMap(initiatives -> evaluate(trx, initiatives));
        } else {
            log.trace("[REWARD] [REWARD_KO] Transaction discarded: {}", trx.getRejectionReasons());
            return Mono.just(rewardTransactionMapper.apply(trx));
        }
    }

    // TODO move in separate service when branch will be aligned
    private Mono<RewardTransactionDTO> evaluate(TransactionDTO trx, List<String> initiatives) {
        String userId = trx.getUserId();
        return userInitiativeCountersRepository.findById(userId)
                .defaultIfEmpty(new UserInitiativeCounters(userId, new HashMap<>()))
                .map(userCounters -> evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters))
                .flatMap(counters2rewardedTrx -> {
                    RewardTransactionDTO rewardedTrx = counters2rewardedTrx.getSecond();
                    userInitiativeCountersUpdateService.update(counters2rewardedTrx.getFirst(), rewardedTrx);

                    return transactionProcessedService.save(rewardedTrx)
                            .then(userInitiativeCountersRepository.save(counters2rewardedTrx.getFirst()))
                            .then(Mono.just(rewardedTrx));
                });
    }

    private Pair<UserInitiativeCounters, RewardTransactionDTO> evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters) {
        RewardTransactionDTO trxRewarded;
        if(BigDecimal.ZERO.compareTo(trx.getEffectiveAmount()) < 0){
            trxRewarded = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);
        } else {
            trxRewarded = rewardTransactionMapper.apply(trx);
            if(OperationType.REFUND.equals(trx.getOperationTypeTranscoded())){
                handleCompleteRefund(trx, trxRewarded);
            } else {
                trxRewarded.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_INVALID_AMOUNT));
                trxRewarded.setStatus(RewardConstants.REWARD_STATE_REJECTED);
            }
        }
        return Pair.of(userCounters, trxRewarded);
    }

    private void handleCompleteRefund(TransactionDTO trx, RewardTransactionDTO trxRewarded) {
        if(trx.getRefundInfo() == null || trx.getRefundInfo().getPreviousRewards().size() == 0){
            trxRewarded.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));
            trxRewarded.setStatus(RewardConstants.REWARD_STATE_REJECTED);
        } else {
            trxRewarded.setRewards(trx.getRefundInfo().getPreviousRewards().entrySet().stream()
                    .map(e->Pair.of(e.getKey(), new Reward(e.getValue().negate())))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
            trxRewarded.setStatus(RewardConstants.REWARD_STATE_REWARDED);
        }
    }
}
