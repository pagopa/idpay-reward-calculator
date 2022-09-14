package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

class Transaction2TransactionProcessedMapperTest {
    Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper = new Transaction2TransactionProcessedMapper();

    @Test
    void mapWithNullTransactionDTO() {
        // When
        TransactionProcessed result = transaction2TransactionProcessedMapper.apply(null);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapWithNotNullTransactionDTO(){
        // Given
        OffsetDateTime trxDate = OffsetDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Rome").getRules().getOffset(LocalDateTime.now()));
        Map<String, Reward> rewards = Map.of(
                "REWARDS0",
                new Reward(BigDecimal.valueOf(100))
        );

        RewardTransactionDTO trx = RewardTransactionDTO.builder()
                .idTrxAcquirer("IDTRXACQUIRER0")
                .acquirerCode("ACQUIRERCODE0")
                .trxDate(trxDate)
                .operationType("OPERATIONTYPE0")
                .correlationId("CORRELATIONID0")
                .amount(BigDecimal.valueOf(100))
                .acquirerId("ACQUIRERID0")
                .userId("USERID0")
                .rewards(rewards)
                .build();

        String id = "IDTRXACQUIRER0ACQUIRERCODE0%sOPERATIONTYPE0ACQUIRERID0".formatted(String.valueOf(trx.getTrxDate().atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime()));

        // When
        TransactionProcessed result = transaction2TransactionProcessedMapper.apply(trx);

        //Then
        Assertions.assertNotNull(result);

        assertCommonFieldValues(trx, result);

        Assertions.assertEquals(id, result.getId());
    }

    private void assertCommonFieldValues(RewardTransactionDTO trx, TransactionProcessed result) {
        LocalDateTime trxDate = trx.getTrxDate().atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime();
        Assertions.assertSame(trx.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertSame(trx.getAcquirerCode(), result.getAcquirerCode());
        Assertions.assertSame(trxDate, result.getTrxDate());
        Assertions.assertSame(trx.getOperationType(), result.getOperationType());
        Assertions.assertSame(trx.getCorrelationId(), result.getCorrelationId());
        Assertions.assertSame(trx.getAmount(), result.getAmount());
        Assertions.assertSame(trx.getAcquirerId(), result.getAcquirerId());
        Assertions.assertEquals(trx.getRewards(), result.getRewards());
        Assertions.assertEquals(trx.getUserId(), result.getUserId());
    }


}