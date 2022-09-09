package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionProcessedServiceImpl implements TransactionProcessedService {
    private final Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper;
    private final TransactionProcessedRepository transactionProcessedRepository;

    public TransactionProcessedServiceImpl(Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper, TransactionProcessedRepository transactionProcessedRepository) {
        this.transaction2TransactionProcessedMapper = transaction2TransactionProcessedMapper;
        this.transactionProcessedRepository = transactionProcessedRepository;
    }

    @Override
    public void saveTransactionProcessed(TransactionDTO trx) {
        TransactionProcessed trxProcessed = transaction2TransactionProcessedMapper.apply(trx);
        transactionProcessedRepository.save(trxProcessed);
    }
}
