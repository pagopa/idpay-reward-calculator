package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionProcessedServiceImpl implements TransactionProcessedService {
    private final Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper;
    private final TransactionProcessedRepository transactionProcessedRepository;

    public TransactionProcessedServiceImpl(Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper, TransactionProcessedRepository transactionProcessedRepository) {
        this.transaction2TransactionProcessedMapper = transaction2TransactionProcessedMapper;
        this.transactionProcessedRepository = transactionProcessedRepository;
    }

    @Override
    public String computeTrxId(TransactionDTO trx) {
        return transaction2TransactionProcessedMapper.computeTrxId(trx);
    }

    @Override
    public Mono<TransactionDTO> checkDuplicateTransactions(TransactionDTO trx) {
        return transactionProcessedRepository.findById(computeTrxId(trx))
                .flatMap(result -> {
                    log.info("[DUPLICATE_TRX] Already processed transaction {}", result.getId());
                    return Mono.<TransactionDTO>error(new IllegalStateException("[DUPLICATE_TRX] Already processed transaction"));
                })
                .defaultIfEmpty(trx)
                .onErrorResume(e -> Mono.empty());
    }

    @Override
    public Mono<TransactionProcessed> save(RewardTransactionDTO trx) {
        TransactionProcessed trxProcessed = transaction2TransactionProcessedMapper.apply(trx);
        trxProcessed.setTimestamp(LocalDateTime.now());
        return transactionProcessedRepository.save(trxProcessed);
    }
}
