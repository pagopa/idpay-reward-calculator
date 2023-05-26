package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaBlockingPartitionConsumer;
import it.gov.pagopa.common.reactive.service.LockService;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.service.reward.trx.TransactionValidatorService;
import it.gov.pagopa.reward.utils.AuditUtilities;
import it.gov.pagopa.reward.utils.RewardConstants;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
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

    private final AuditUtilities auditUtilities;

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
            AuditUtilities auditUtilities,

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
        this.auditUtilities = auditUtilities;

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
        setMessageData(payload, message);

        return Mono.just(payload)
                .map(transactionValidatorService::validate)
                .flatMap(transactionProcessedService::checkDuplicateTransactions)
                .flatMap(operationTypeHandlerService::handleOperationType)
                .flatMap(this::retrieveInitiativesAndEvaluate)
                .doOnNext(r -> {
                    lockReleaser.accept(null);
                    rewardNotifierService.notifyFallbackToErrorTopic(r);
                    auditUtilities.logExecute(r);
                });
    }

    private void setMessageData(TransactionDTO trx, Message<String> message) {
        if(StringUtils.isEmpty(trx.getChannel())){
            trx.setChannel(RewardConstants.TRX_CHANNEL_RTD);
        }
        trx.setRuleEngineTopicPartition(CommonUtilities.getHeaderValue(message, KafkaHeaders.RECEIVED_PARTITION_ID));
        trx.setRuleEngineTopicOffset(CommonUtilities.getHeaderValue(message, KafkaHeaders.OFFSET));
    }

    @Override
    protected int getMessagePartitionKey(Message<String> message) {
        String userId=Utils.readUserId( CommonUtilities.readMessagePayload(message));
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
    public String getFlowName() {
        return "REWARD";
    }

    private Mono<RewardTransactionDTO> retrieveInitiativesAndEvaluate(TransactionDTO trx) {
        if (CollectionUtils.isEmpty(trx.getRejectionReasons())) {
            return onboardedInitiativesService.getInitiatives(trx)
                    .collectList()
                    .flatMap(initiatives -> initiativesEvaluatorFacadeService.evaluateAndUpdateBudget(trx, initiatives));
        } else {
            log.trace("[REWARD] [REWARD_KO] Transaction discarded: {} - {}", trx.getId(), trx.getRejectionReasons());
            RewardTransactionDTO rejectedTrx = rewardTransactionMapper.apply(trx);
            return transactionProcessedService.save(rejectedTrx)
                    .then(Mono.just(rejectedTrx));
        }
    }


}
