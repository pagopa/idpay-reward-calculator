package it.gov.pagopa.reward.test.fakers;

import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionDroolsMapper;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.test.fakers.rule.TransactionDTOFaker;

public class TransactionDroolsDtoFaker {
    private static final Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper = new Transaction2TransactionDroolsMapper();

    /**
     * It will return an example of {@link TransactionDroolsDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static TransactionDroolsDTO mockInstance(Integer bias) {
        return transaction2TransactionDroolsMapper.apply(TransactionDTOFaker.mockInstance(bias));
    }
}
