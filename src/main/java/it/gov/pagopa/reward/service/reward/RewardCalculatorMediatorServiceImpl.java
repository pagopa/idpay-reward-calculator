package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
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
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
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
    protected Mono<RewardTransactionDTO> execute(TransactionDTO payload, Message<String> message) {
        throw new IllegalStateException("Logic overridden");
    }

    @Override
    protected Mono<RewardTransactionDTO> execute(Message<String> message) {
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
                .flatMap(this::executeAfterLock);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyTransactionEvaluation(message, "[REWARD] Unexpected JSON", true, e);
    }

    private Mono<RewardTransactionDTO> executeAfterLock(Pair<Message<String>, Integer> messageAndLockId) {
        final long startTime = System.currentTimeMillis();
        final Message<String> message = messageAndLockId.getKey();

        final Consumer<? super RewardTransactionDTO> lockReleaser = x -> {
            int lockId = messageAndLockId.getValue();
            if (lockId > -1) {
                lockService.releaseLock(lockId);
                messageAndLockId.setValue(-1);
                log.debug("[REWARD] [LOCK_RELEASED] released lock having id {}", lockId);
            }
        };

        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMap(transactionProcessedService::checkDuplicateTransactions)
                .flatMap(operationTypeHandlerService::handleOperationType)
                .map(transactionValidatorService::validate)
                .flatMap(this::retrieveInitiativesAndEvaluate)
                .doOnNext(lockReleaser)
                .doOnNext(r -> {
                    Exception exception=null;
                    try{
                        if(!rewardNotifierService.notify(r)){
                            exception = new IllegalStateException("Something gone wrong while reward notify");
                        }
                    } catch (Exception e){
                        exception = e;
                    }
                    if (exception != null) {
                        log.error("[UNEXPECTED_TRX_PROCESSOR_ERROR] Unexpected error occurred publishing rewarded transaction: {}", r);
                        errorNotifierService.notifyRewardedTransaction(new GenericMessage<>(r, message.getHeaders()), "An error occurred while publishing the transaction evaluation result", true, exception);
                    }
                })

                .doFinally(x -> {
                    lockReleaser.accept(null);
                    log.info("[PERFORMANCE_LOG] [REWARD] - Time between before and after evaluate message {} ms with payload: {}", System.currentTimeMillis() - startTime, message.getPayload());
                });
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
