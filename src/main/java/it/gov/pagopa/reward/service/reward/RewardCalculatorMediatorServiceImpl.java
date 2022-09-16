package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.LockService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService {

    private final LockService lockService;
    private final TransactionProcessedService transactionProcessedService;
    private final OperationTypeHandlerService operationTypeHandlerService;
    private final TransactionValidatorService transactionValidatorService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;
    private final Transaction2RewardTransactionMapper rewardTransactionMapper;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardCalculatorMediatorServiceImpl(LockService lockService, TransactionProcessedService transactionProcessedService, OperationTypeHandlerService operationTypeHandlerService, TransactionValidatorService transactionValidatorService, OnboardedInitiativesService onboardedInitiativesService, InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService, Transaction2RewardTransactionMapper rewardTransactionMapper, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.lockService = lockService;
        this.transactionProcessedService = transactionProcessedService;
        this.operationTypeHandlerService = operationTypeHandlerService;
        this.transactionValidatorService = transactionValidatorService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.rewardTransactionMapper = rewardTransactionMapper;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(TransactionDTO.class);
    }

    @Override
    public Flux<RewardTransactionDTO> execute(Flux<Message<String>> messageFlux) {
        return messageFlux
                .flatMap(trx -> Mono.fromSupplier(() -> {
                    int lockId=-1;
                    String userId= Utils.readUserId(trx.getPayload());
                    if(!StringUtils.isEmpty(userId)) {
                        lockId = calculateLockId(userId);
                        lockService.acquireLock(lockId);
                        log.debug("[REWARD] [LOCK_ACQUIRED] trx having userId {} acquired lock having id {}", userId, lockId);
                    }
                    return Pair.of(trx, lockId);
                }))
                .flatMap(this::execute);
    }

    public Mono<RewardTransactionDTO> execute(Pair<Message<String>, Integer> messageAndLockId) {
        final long startTime = System.currentTimeMillis();
        final Message<String> message = messageAndLockId.getFirst();

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
                .doFinally(x -> {
                    int lockId = messageAndLockId.getSecond();
                    if(lockId>-1) {
                        lockService.releaseLock(lockId);
                        log.debug("[REWARD] [LOCK_RELEASED] released lock having id {}", lockId);
                    }
                    log.info("[PERFORMANCE_LOG] [REWARD] - Time between before and after evaluate message {} ms with payload: {}", System.currentTimeMillis() - startTime, message.getPayload());
                });
    }

    private TransactionDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyTransactionEvaluation(message, "Unexpected JSON", true, e));
    }

    public int calculateLockId(String userId) {
        return Math.floorMod(userId.hashCode(), lockService.getBuketSize());
    }

    private Mono<RewardTransactionDTO> retrieveInitiativesAndEvaluate(TransactionDTO trx) {
        if (CollectionUtils.isEmpty(trx.getRejectionReasons())) {
            return onboardedInitiativesService.getInitiatives(trx)
                    .collectList()
                    .flatMap(initiatives -> initiativesEvaluatorFacadeService.evaluate(trx, initiatives));
        } else {
            log.trace("[REWARD] [REWARD_KO] Transaction discarded: {}", trx.getRejectionReasons());
            return Mono.just(rewardTransactionMapper.apply(trx));
        }
    }


}
