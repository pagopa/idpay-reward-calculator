package it.gov.pagopa.dto.mapper;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.model.RewardTransaction;
import org.springframework.stereotype.Service;

@Service
public class TransactionMapper {
    public RewardTransaction map(TransactionDTO transaction) {

        RewardTransaction rewardTrx = null;

        if (transaction != null) {
            rewardTrx = RewardTransaction.builder().build();
            rewardTrx.setIdTrxAcquirer(transaction.getIdTrxAcquirer());
            rewardTrx.setAcquirerCode(transaction.getAcquirerCode());
            rewardTrx.setTrxDate(transaction.getTrxDate());
            rewardTrx.setHpan(transaction.getHpan());
            rewardTrx.setOperationType(transaction.getOperationType());
            rewardTrx.setCircuitType(transaction.getCircuitType());
            rewardTrx.setIdTrxIssuer(transaction.getIdTrxIssuer());
            rewardTrx.setCorrelationId(transaction.getCorrelationId());
            rewardTrx.setAmount(transaction.getAmount());
            rewardTrx.setAmountCurrency(transaction.getAmountCurrency());
            rewardTrx.setMcc(transaction.getMcc());
            rewardTrx.setAcquirerId(transaction.getAcquirerId());
            rewardTrx.setMerchantId(transaction.getMerchantId());
            rewardTrx.setTerminalId(transaction.getTerminalId());
            rewardTrx.setBin(transaction.getBin());
            rewardTrx.setSenderCode(transaction.getSenderCode());
            rewardTrx.setFiscalCode(transaction.getFiscalCode());
            rewardTrx.setVat(transaction.getVat());
            rewardTrx.setPosType(transaction.getPosType());
            rewardTrx.setPar(transaction.getPar());
        }
        return rewardTrx;
    }

    public RewardTransactionDTO mapDTO(TransactionDTO transaction) {

        RewardTransactionDTO rewardTrx = null;

        if (transaction != null) {
            rewardTrx = RewardTransactionDTO.builder().build();
            rewardTrx.setIdTrxAcquirer(transaction.getIdTrxAcquirer());
            rewardTrx.setAcquirerCode(transaction.getAcquirerCode());
            rewardTrx.setTrxDate(transaction.getTrxDate());
            rewardTrx.setHpan(transaction.getHpan());
            rewardTrx.setOperationType(transaction.getOperationType());
            rewardTrx.setCircuitType(transaction.getCircuitType());
            rewardTrx.setIdTrxIssuer(transaction.getIdTrxIssuer());
            rewardTrx.setCorrelationId(transaction.getCorrelationId());
            rewardTrx.setAmount(transaction.getAmount());
            rewardTrx.setAmountCurrency(transaction.getAmountCurrency());
            rewardTrx.setMcc(transaction.getMcc());
            rewardTrx.setAcquirerId(transaction.getAcquirerId());
            rewardTrx.setMerchantId(transaction.getMerchantId());
            rewardTrx.setTerminalId(transaction.getTerminalId());
            rewardTrx.setBin(transaction.getBin());
            rewardTrx.setSenderCode(transaction.getSenderCode());
            rewardTrx.setFiscalCode(transaction.getFiscalCode());
            rewardTrx.setVat(transaction.getVat());
            rewardTrx.setPosType(transaction.getPosType());
            rewardTrx.setPar(transaction.getPar());
        }
        return rewardTrx;
    }
}
