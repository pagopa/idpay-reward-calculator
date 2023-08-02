package it.gov.pagopa.reward.test.fakers;

import it.gov.pagopa.reward.dto.mapper.trx.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class RewardTransactionDTOFaker {

    private static final TransactionDroolsDTO2RewardTransactionMapper mapper = new TransactionDroolsDTO2RewardTransactionMapper();

    /**
     * It will return an example of {@link RewardTransactionDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static RewardTransactionDTO mockInstance(Integer bias) {
        TransactionDroolsDTO trx = TransactionDroolsDtoFaker.mockInstance(bias);
        String initiativeId = "INITIATIVEID%d".formatted(bias);
        trx.setRewards(Map.of(
                initiativeId, new Reward(initiativeId, "ORGID", trx.getEffectiveAmount().divide(BigDecimal.TEN, RoundingMode.CEILING))
        ));
        trx.setInitiatives(List.of(initiativeId));
        return mapper.apply(trx);
    }
}
