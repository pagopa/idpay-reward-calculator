package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.service.reward.TrxNotifierService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class TransactionProcessedServiceImpl implements TransactionProcessedService {

    private final OperationTypeHandlerService operationTypeHandlerService;
    private final Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper;
    private final TransactionProcessedRepository transactionProcessedRepository;
    private final TrxNotifierService trxNotifierService;

    public TransactionProcessedServiceImpl(OperationTypeHandlerService operationTypeHandlerService, Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper, TransactionProcessedRepository transactionProcessedRepository, TrxNotifierService trxNotifierService) {
        this.operationTypeHandlerService = operationTypeHandlerService;
        this.transaction2TransactionProcessedMapper = transaction2TransactionProcessedMapper;
        this.transactionProcessedRepository = transactionProcessedRepository;
        this.trxNotifierService = trxNotifierService;
    }

    @Override
    public Mono<TransactionDTO> checkDuplicateTransactions(TransactionDTO trx) {
        boolean isChargeOperation = operationTypeHandlerService.isChargeOperation(trx);
        if(isChargeOperation && !StringUtils.isEmpty(trx.getCorrelationId())){
            log.debug("[REWARD] Recognized charge op with a correlationId, checking for duplicate and refund ops");
            return transactionProcessedRepository.findByAcquirerIdAndCorrelationId(trx.getAcquirerId(), trx.getCorrelationId())
                    .collectList()
                    .flatMap(trxs -> checkCorrelatedTransactions(trx, trxs));
        } else {
            return transactionProcessedRepository.findById(trx.getId())
                    .flatMap(result -> {
                        if(!isChargeOperation && result.getRejectionReasons().contains(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH)){
                            log.info("[REWARD][REFUND_RECOVER] Retrieved recovered refund {}", result.getId());
                            return Mono.empty();
                        } else {
                            log.info("[REWARD][DUPLICATE_TRX] Already processed transaction found searching by id {}", result.getId());
                            return Mono.<TransactionDTO>error(new IllegalStateException("[DUPLICATE_TRX] Already processed transaction found searching by id"));
                        }
                    })
                    .defaultIfEmpty(trx)
                    .onErrorResume(e -> Mono.empty())
                    .doOnNext(x -> log.trace("[REWARD] Duplicate check by id successful ended: {}", trx.getId()));
        }
    }

    private Mono<TransactionDTO> checkCorrelatedTransactions(TransactionDTO trx, List<TransactionProcessed> trxs) {
        for (TransactionProcessed t : trxs) {
            if (trx.getId().equals(t.getId())) {
                log.info("[REWARD][DUPLICATE_TRX] Already processed transaction found searching by acquirerId and correlationId {}: acquirerId: {}; correlationId: {}", trx.getId(), trx.getAcquirerId(), trx.getCorrelationId());
                return Mono.empty();
            } else if(OperationType.CHARGE.equals(t.getOperationTypeTranscoded())){
                log.info("[REWARD][DUPLICATE_CORRELATIONID] Found an other CHARGE transaction having same acquirerId and correlationId trx id {} retrieved id {}: acquirerId: {}; correlationId: {}", trx.getId(), t.getId(), trx.getAcquirerId(), trx.getCorrelationId());
                trx.getRejectionReasons()
                        .add(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID);
                return Mono.just(trx);
            }
        }

        for (TransactionProcessed r : trxs) {
            log.info("[REWARD][REFUND_RECOVER] Recovering refund related to current trx; trx id {}; refund id {}; acquirerId: {}; correlationId: {}", trx.getId(), r.getId(), trx.getAcquirerId(), trx.getCorrelationId());
            if(!trxNotifierService.notify(r)){
                return Mono.error(new IllegalStateException("[REWARD][REFUND_RECOVER] Something gone wrong while recovering previous refund; trxId %s refundId %s".formatted(trx.getId(), r.getId())));
            }
        }

        return Mono.just(trx);
    }

    @Override
    public Mono<TransactionProcessed> save(RewardTransactionDTO trx) {
        //TODO if refund not match, store the entire payload
        TransactionProcessed trxProcessed = transaction2TransactionProcessedMapper.apply(trx);
        trxProcessed.setTimestamp(LocalDateTime.now());
        return transactionProcessedRepository.save(trxProcessed);
    }
}
