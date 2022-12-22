package it.gov.pagopa.reward.service.reward.trx;

import com.mongodb.assertions.Assertions;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TransactionValidatorServiceImpl implements TransactionValidatorService {

    @Override
    public TransactionDTO validate(TransactionDTO transactionDTO) {
        if(BigDecimal.ZERO.compareTo(transactionDTO.getAmount()) >= 0){
            transactionDTO.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_INVALID_AMOUNT);
        }

        Assertions.assertNotNull(transactionDTO.getId());

        transactionDTO.setAmount(centsToEur(transactionDTO.getAmount()));
        return transactionDTO;
    }

    private BigDecimal centsToEur(BigDecimal amount) {
        return amount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);
    }
}
