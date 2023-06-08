package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
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
            Transaction2RewardTransactionMapper.copyFields(transaction, rewardTrx);
        }
        return rewardTrx;
    }
}
