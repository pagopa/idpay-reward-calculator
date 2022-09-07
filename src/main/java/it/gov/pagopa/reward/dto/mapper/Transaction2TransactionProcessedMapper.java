package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Transaction2TransactionProcessedMapper implements Function<TransactionDTO, TransactionProcessed> {
    @Override
    public TransactionProcessed apply(TransactionDTO transaction) {

        TransactionProcessed trxProcessed = null;

        if (transaction != null) {
            trxProcessed = new TransactionProcessed();
            trxProcessed.setIdTrxAcquirer(transaction.getIdTrxAcquirer());
            trxProcessed.setAcquirerCode(transaction.getAcquirerCode());
            trxProcessed.setTrxDate(transaction.getTrxDate());
            trxProcessed.setOperationType(transaction.getOperationType());
            trxProcessed.setAcquirerId(transaction.getAcquirerId());
        }
        return trxProcessed;
    }
}