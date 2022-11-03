package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.service.BaseKafkaConsumer;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.LockService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.service.reward.trx.TransactionValidatorService;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl extends BaseKafkaConsumer<TransactionDTO, RewardTransactionDTO> implements RewardCalculatorMediatorService {

    private final LockService lockService;
    private final TransactionProcessedService transactionProcessedService;
    private final OperationTypeHandlerService operationTypeHandlerService;
    private final TransactionValidatorService transactionValidatorService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;
    private final Transaction2RewardTransactionMapper rewardTransactionMapper;
    private final RewardNotifierService rewardNotifierService;
    private final ErrorNotifierService errorNotifierService;

    private final Duration commitDelay;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardCalculatorMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            LockService lockService,
            TransactionProcessedService transactionProcessedService,
            OperationTypeHandlerService operationTypeHandlerService,
            TransactionValidatorService transactionValidatorService,
            OnboardedInitiativesService onboardedInitiativesService,
            InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService,
            Transaction2RewardTransactionMapper rewardTransactionMapper,
            RewardNotifierService rewardNotifierService,
            ErrorNotifierService errorNotifierService,

            @Value("${spring.cloud.stream.kafka.bindings.trxProcessor-in-0.consumer.ackTime}") long commitMillis,

            ObjectMapper objectMapper) {
        super(applicationName);
        this.lockService = lockService;
        this.transactionProcessedService = transactionProcessedService;
        this.operationTypeHandlerService = operationTypeHandlerService;
        this.transactionValidatorService = transactionValidatorService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.rewardTransactionMapper = rewardTransactionMapper;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.rewardNotifierService = rewardNotifierService;
        this.errorNotifierService = errorNotifierService;
        this.commitDelay = Duration.ofMillis(commitMillis);

        this.objectReader = objectMapper.readerFor(TransactionDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<RewardTransactionDTO>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(p -> log.debug("[REWARD] Processed offsets committed successfully"));
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyTransactionEvaluation(message, "[REWARD] An error occurred evaluating transaction", true, e);
    }

    @Override
    protected Mono<RewardTransactionDTO> execute(TransactionDTO payload, Message<String> message, Map<String, Object> ctx) {
        throw new IllegalStateException("Logic overridden");
    }

    @Override
    protected Mono<RewardTransactionDTO> execute(Message<String> message, Map<String, Object> ctx) {
        return Mono.fromSupplier(() -> {
                    int lockId = -1;
                    String userId = Utils.readUserId(message.getPayload());
                    if (!StringUtils.isEmpty(userId)) {
                        lockId = calculateLockId(userId);
                        lockService.acquireLock(lockId);
                        log.debug("[REWARD] [LOCK_ACQUIRED] trx having userId {} acquired lock having id {}", userId, lockId);
                    }
                    return new MutablePair<>(message, lockId);
                })
                .flatMap(m -> executeAfterLock(m, ctx));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyTransactionEvaluation(message, "[REWARD] Unexpected JSON", true, e);
    }

    private Mono<RewardTransactionDTO> executeAfterLock(Pair<Message<String>, Integer> messageAndLockId, Map<String, Object> ctx) {
        log.trace("[REWARD] Received payload: {}", messageAndLockId.getKey().getPayload());

        final Message<String> message = messageAndLockId.getKey();

        final Consumer<? super Signal<RewardTransactionDTO>> lockReleaser = x -> {
            int lockId = messageAndLockId.getValue();
            if (lockId > -1) {
                lockService.releaseLock(lockId);
                messageAndLockId.setValue(-1);
                log.debug("[REWARD] [LOCK_RELEASED] released lock having id {}", lockId);
            }
        };

        ctx.put(CONTEXT_KEY_START_TIME, System.currentTimeMillis());

        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .map(transactionValidatorService::validate)
                .flatMap(transactionProcessedService::checkDuplicateTransactions)
                .flatMap(operationTypeHandlerService::handleOperationType)
                .flatMap(this::retrieveInitiativesAndEvaluate)
                .doOnEach(lockReleaser)

                .doOnNext(r -> {
                    try {
                        if (!rewardNotifierService.notify(r)) {
                            throw new IllegalStateException("[REWARD] Something gone wrong while reward notify");
                        }
                    } catch (Exception e) {
                        log.error("[UNEXPECTED_TRX_PROCESSOR_ERROR] Unexpected error occurred publishing rewarded transaction: {}", r);
                        errorNotifierService.notifyRewardedTransaction(RewardNotifierServiceImpl.buildMessage(r), "[REWARD] An error occurred while publishing the transaction evaluation result", true, e);
                    }
                });
    }

    @Override
    protected String getFlowName() {
        return "REWARD";
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
            log.trace("[REWARD] [REWARD_KO] Transaction discarded: {} - {}", trx.getId(), trx.getRejectionReasons());
            return Mono.just(rewardTransactionMapper.apply(trx));
        }
    }


}
