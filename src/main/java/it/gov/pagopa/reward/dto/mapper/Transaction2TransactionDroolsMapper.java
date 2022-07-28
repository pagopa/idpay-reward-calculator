package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Transaction2TransactionDroolsMapper implements Function<TransactionDTO, TransactionDroolsDTO> {
    @Override
    public TransactionDroolsDTO apply(TransactionDTO transaction) {

        TransactionDroolsDTO rewardTrx = null;

        if (transaction != null) {
            rewardTrx = new TransactionDroolsDTO();
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
