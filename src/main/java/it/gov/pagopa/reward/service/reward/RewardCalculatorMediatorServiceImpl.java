package it.gov.pagopa.reward.service.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService {

    private final TransactionProcessedService transactionProcessedService;
    private final OperationTypeHandlerService operationTypeHandlerService;
    private final TransactionValidatorService transactionValidatorService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;
    private final Transaction2RewardTransactionMapper rewardTransactionMapper;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardCalculatorMediatorServiceImpl(TransactionProcessedService transactionProcessedService, OperationTypeHandlerService operationTypeHandlerService, TransactionValidatorService transactionValidatorService, OnboardedInitiativesService onboardedInitiativesService, InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService, Transaction2RewardTransactionMapper rewardTransactionMapper, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
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
                    .flatMap(initiatives -> initiativesEvaluatorFacadeService.evaluate(trx, initiatives));
        } else {
            log.trace("[REWARD] [REWARD_KO] Transaction discarded: {}", trx.getRejectionReasons());
            return Mono.just(rewardTransactionMapper.apply(trx));
        }
    }


}
