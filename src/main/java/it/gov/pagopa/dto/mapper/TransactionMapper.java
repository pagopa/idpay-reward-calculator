package it.gov.pagopa.dto.mapper;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.model.TransactionPrize;
import org.springframework.stereotype.Service;

@Service
public class TransactionMapper {
    public TransactionPrize map(TransactionDTO transaction) {

        TransactionPrize trxPrize = null;

        if (transaction != null) {
            trxPrize = TransactionPrize.builder().build();
            trxPrize.setIdTrxAcquirer(transaction.getIdTrxAcquirer());
            trxPrize.setAcquirerCode(transaction.getAcquirerCode());
            trxPrize.setTrxDate(transaction.getTrxDate());
            trxPrize.setHpan(transaction.getHpan());
            trxPrize.setOperationType(transaction.getOperationType());
            trxPrize.setCircuitType(transaction.getCircuitType());
            trxPrize.setIdTrxIssuer(transaction.getIdTrxIssuer());
            trxPrize.setCorrelationId(transaction.getCorrelationId());
            trxPrize.setAmount(transaction.getAmount());
            trxPrize.setAmountCurrency(transaction.getAmountCurrency());
            trxPrize.setMcc(transaction.getMcc());
            trxPrize.setAcquirerId(transaction.getAcquirerId());
            trxPrize.setMerchantId(transaction.getMerchantId());
            trxPrize.setTerminalId(transaction.getTerminalId());
            trxPrize.setBin(transaction.getBin());
        }

        return trxPrize;

    }
}
