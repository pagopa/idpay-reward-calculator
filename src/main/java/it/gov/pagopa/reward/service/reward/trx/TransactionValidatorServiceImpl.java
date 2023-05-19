package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class TransactionValidatorServiceImpl implements TransactionValidatorService {

    @Override
    public TransactionDTO validate(TransactionDTO transactionDTO) {
        Objects.requireNonNull(transactionDTO.getId());

        validateAmount(transactionDTO);

        return transactionDTO;
    }

    private void validateAmount(TransactionDTO transactionDTO) {
        if(transactionDTO.getAmountCents()==null){
            transactionDTO.setAmountCents(transactionDTO.getAmount().longValue());
        }
        transactionDTO.setAmount(CommonUtilities.centsToEuro(transactionDTO.getAmountCents()));

        if(BigDecimal.ZERO.compareTo(transactionDTO.getAmount()) >= 0){
            transactionDTO.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_INVALID_AMOUNT);
        }
    }

}
