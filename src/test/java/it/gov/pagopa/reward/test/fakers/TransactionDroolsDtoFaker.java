package it.gov.pagopa.reward.test.fakers;

import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionDroolsMapper;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;

public class TransactionDroolsDtoFaker {
    private static final Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper = new Transaction2TransactionDroolsMapper();

    /**
     * It will return an example of {@link TransactionDroolsDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static TransactionDroolsDTO mockInstance(Integer bias) {
        final TransactionDroolsDTO trx = transaction2TransactionDroolsMapper.apply(TransactionDTOFaker.mockInstance(bias));
        trx.setTrxChargeDate(trx.getTrxDate());
        trx.setEffectiveAmount(trx.getAmount());
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        return trx;
    }
}
