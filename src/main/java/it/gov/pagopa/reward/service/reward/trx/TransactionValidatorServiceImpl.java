package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionValidatorServiceImpl implements TransactionValidatorService {
    @Override
    public TransactionDTO validate(TransactionDTO transactionDTO) {
        if(BigDecimal.ZERO.compareTo(transactionDTO.getAmount()) >= 0){
            transactionDTO.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_INVALID_AMOUNT);
        }
        return transactionDTO;
    }
}
