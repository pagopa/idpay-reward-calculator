package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

class TransactionProcessed2SyncTrxResponseDTOMapperTest {
    @Test
    void applyTest(){
        // Given
        String initiativeId = "INITIATIVEID";
        Reward reward = Reward.builder()
                .initiativeId(initiativeId)
                .providedReward(BigDecimal.ONE)
                .build();
        TransactionProcessed transactionProcessed = TransactionProcessed.builder()
                .id("TRXID")
                .channel("CHANNEL")
                .userId("USERID")
                .status("STATUS")
                .rewards(Map.of(initiativeId,reward))
                .build();

        TransactionProcessed2SyncTrxResponseDTOMapper mapper = new TransactionProcessed2SyncTrxResponseDTOMapper();
        // When
        SynchronousTransactionResponseDTO result = mapper.apply(transactionProcessed, initiativeId);
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(transactionProcessed.getId(), result.getTransactionId());
        Assertions.assertEquals(transactionProcessed.getChannel(), result.getChannel());
        Assertions.assertEquals(initiativeId, result.getInitiativeId());
        Assertions.assertEquals(transactionProcessed.getUserId(), result.getUserId());
        Assertions.assertEquals(transactionProcessed.getStatus(), result.getStatus());
        Assertions.assertEquals(reward, result.getReward());
        TestUtils.checkNotNullFields(result, "rejectionReasons");
    }

}