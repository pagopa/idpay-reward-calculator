package it.gov.pagopa.reward.dto.mapper.trx.recover;

import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

class RecoveredTrx2RewardTransactionMapperTest {

    private final Transaction2TransactionProcessedMapper transactionProcessedMapper = new Transaction2TransactionProcessedMapper();

    private final RecoveredTrx2RewardTransactionMapper mapper = new RecoveredTrx2RewardTransactionMapper(new Transaction2RewardTransactionMapper());

    @Test
    void test(){
        // Given
        RewardTransactionDTO expected = RewardTransactionDTOFaker.mockInstance(0);
        expected.setStatus(RewardConstants.REWARD_STATE_REWARDED);
        expected.setRejectionReasons(List.of("DUMMY"));
        expected.setInitiativeRejectionReasons(Map.of("INITIATIVEID", List.of("DUMMY")));
        expected.setRewards(Map.of("INITIATIVEID2", new Reward()));
        expected.setElaborationDateTime(LocalDateTime.now());

        TransactionProcessed trxProcessed = transactionProcessedMapper.apply(expected);
        trxProcessed.setElaborationDateTime(expected.getElaborationDateTime());
        trxProcessed.setRefundInfo(new RefundInfo());

        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);
        trx.setAmount(expected.getAmount());

        // When
        RewardTransactionDTO result = mapper.apply(trx, trxProcessed);

        // Then
        Assertions.assertEquals(expected, result);

        TestUtils.checkNotNullFields(result,"businessName");
    }
}
