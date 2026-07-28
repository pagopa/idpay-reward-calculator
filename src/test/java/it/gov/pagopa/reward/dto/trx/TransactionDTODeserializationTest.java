package it.gov.pagopa.reward.dto.trx;

import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

class TransactionDTODeserializationTest {

    @Test
    void shouldDeserializeTrxDateWithoutOffsetUsingEuropeRomeZone() {
        String payload = """
                {
                  "trxDate": "2026-07-16T17:01:15.098568"
                }
                """;

        RewardTransactionDTO result = TestUtils.objectMapper.readValue(payload, RewardTransactionDTO.class);

        OffsetDateTime expected = LocalDateTime.parse("2026-07-16T17:01:15.098568")
                .atZone(ZoneId.of("Europe/Rome"))
                .toOffsetDateTime();

        Assertions.assertNotNull(result.getTrxDate());
        Assertions.assertEquals(expected, result.getTrxDate());
    }

    @Test
    void shouldDeserializeTrxDateWithOffset() {
        String payload = """
                {
                  "trxDate": "2026-07-14T17:58:07.132+02:00"
                }
                """;

        RewardTransactionDTO result = TestUtils.objectMapper.readValue(payload, RewardTransactionDTO.class);

        Assertions.assertNotNull(result.getTrxDate());
        Assertions.assertEquals(OffsetDateTime.parse("2026-07-14T17:58:07.132+02:00"), result.getTrxDate());
    }
}

