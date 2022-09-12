package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
    public Mono<TransactionProcessed> getProcessedTransactions(String trxId) {
        return transactionProcessedRepository.findById(trxId);
    }

    @Override
    public Mono<TransactionProcessed> save(RewardTransactionDTO trx) {
        TransactionProcessed trxProcessed = transaction2TransactionProcessedMapper.apply(trx);
        return transactionProcessedRepository.save(trxProcessed);
    }
}
