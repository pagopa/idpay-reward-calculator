package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.service.BaseKafkaBlockingPartitionConsumer;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.LockService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.service.reward.trx.TransactionValidatorService;
import it.gov.pagopa.reward.utils.Utilities;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
public class RewardCalculatorMediatorServiceImpl extends BaseKafkaBlockingPartitionConsumer<TransactionDTO, RewardTransactionDTO> implements RewardCalculatorMediatorService {

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

    private final Utilities utilities;

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
            Utilities utilities,

            @Value("${spring.cloud.stream.kafka.bindings.trxProcessor-in-0.consumer.ackTime}") long commitMillis,

            ObjectMapper objectMapper) {
        super(applicationName, lockService);
        this.transactionProcessedService = transactionProcessedService;
        this.operationTypeHandlerService = operationTypeHandlerService;
        this.transactionValidatorService = transactionValidatorService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.rewardTransactionMapper = rewardTransactionMapper;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.rewardNotifierService = rewardNotifierService;
        this.errorNotifierService = errorNotifierService;
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.utilities = utilities;

        this.objectReader = objectMapper.readerFor(TransactionDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<RewardTransactionDTO>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(p -> log.info("[REWARD] Processed offsets committed successfully"));
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyTransactionEvaluation(message, "[REWARD] An error occurred evaluating transaction", true, e);
    }

    @Override
    protected Mono<RewardTransactionDTO> execute(TransactionDTO payload, Message<String> message, Map<String, Object> ctx) {
        throw new IllegalStateException("Logic overridden by execute(TransactionDTO, Message<java.lang.String>, Map<>, lockReleaser)");
    }

    @Override
    protected Mono<RewardTransactionDTO> execute(TransactionDTO payload, Message<String> message, Map<String, Object> ctx, Consumer<? super Signal<RewardTransactionDTO>> lockReleaser) {
        return Mono.just(payload)
                .map(transactionValidatorService::validate)
                .flatMap(transactionProcessedService::checkDuplicateTransactions)
                .flatMap(operationTypeHandlerService::handleOperationType)
                .flatMap(this::retrieveInitiativesAndEvaluate)
                .doOnEach(lockReleaser)
                .doOnNext(r -> {
                    try {
                        if (OperationType.CHARGE.equals(payload.getOperationTypeTranscoded())) {
                            utilities.logCharge(payload.getUserId(), payload.getIdTrxIssuer(), payload.getIdTrxAcquirer(),
                                    r.getRewards().entrySet().stream().map(entry -> ("initiativeId=".concat(entry.getKey())
                                            .concat(" reward=").concat(entry.getValue().getProvidedReward().toString()))).toString());
                        } else if (OperationType.REFUND.equals(payload.getOperationTypeTranscoded())) {
                            utilities.logRefund(payload.getUserId(), payload.getIdTrxIssuer(), payload.getIdTrxAcquirer(),
                                    r.getRewards().entrySet().stream().map(entry -> ("initiativeId=".concat(entry.getKey())
                                            .concat(" reward=").concat(entry.getValue().getProvidedReward().toString()))).toString(), payload.getCorrelationId());
                        }
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
    protected int getMessagePartitionKey(Message<String> message) {
        String userId=Utils.readUserId(message.getPayload());
        if(!StringUtils.isEmpty(userId)){
            return userId.hashCode();
        } else {
            return super.getMessagePartitionKey(message);
        }
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyTransactionEvaluation(message, "[REWARD] Unexpected JSON", true, e);
    }

    @Override
    protected String getFlowName() {
        return "REWARD";
    }

    private Mono<RewardTransactionDTO> retrieveInitiativesAndEvaluate(TransactionDTO trx) {
        if (CollectionUtils.isEmpty(trx.getRejectionReasons())) {
            return onboardedInitiativesService.getInitiatives(trx)
                    .collectList()
                    .flatMap(initiatives -> initiativesEvaluatorFacadeService.evaluate(trx, initiatives));
        } else {
            log.trace("[REWARD] [REWARD_KO] Transaction discarded: {} - {}", trx.getId(), trx.getRejectionReasons());
            RewardTransactionDTO rejectedTrx = rewardTransactionMapper.apply(trx);
            return transactionProcessedService.save(rejectedTrx)
                    .then(Mono.just(rejectedTrx));
        }
    }


}
