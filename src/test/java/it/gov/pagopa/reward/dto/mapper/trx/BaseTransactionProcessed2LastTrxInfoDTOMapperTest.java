package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.dto.trx.LastTrxInfoDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BaseTransactionProcessed2LastTrxInfoDTOMapperTest {
    private final BaseTransactionProcessed2LastTrxInfoDTOMapper mapper = new BaseTransactionProcessed2LastTrxInfoDTOMapper();

    @Test
    void apply_rewardTransactionDTO2LastTrxInfoDTO() {
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(1);
        rewardTransactionDTO.setId("TRXID");

        LastTrxInfoDTO result = mapper.apply(rewardTransactionDTO);

        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
    }

    @Test
    void apply_TransactionProcessed2LastTrxInfoDTO() {
        TransactionProcessed transactionProcessed = TransactionProcessedFaker.mockInstance(1);

        LastTrxInfoDTO result = mapper.apply(transactionProcessed);

        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
    }
}