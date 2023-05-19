package it.gov.pagopa.common.utils;

import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

class CommonUtilitiesTest {

    @Test
    void getHeaderValueTest(){
        Message<String> msg = MessageBuilder
                .withPayload("")
                .setHeader("HEADERNAME", "HEADERVALUE".getBytes(StandardCharsets.UTF_8))
                .build();
        Assertions.assertNull(CommonUtilities.getByteArrayHeaderValue(msg, "NOTEXISTS"));
        Assertions.assertEquals("HEADERVALUE", CommonUtilities.getByteArrayHeaderValue(msg, "HEADERNAME"));
    }

    @Test
    void centsToEuroTest(){
        Assertions.assertEquals(
                BigDecimal.valueOf(5).setScale(2, RoundingMode.UNNECESSARY),
                CommonUtilities.centsToEuro(5_00L)
        );
    }

    @Test
    void euro2CentsTest(){
        Assertions.assertEquals(
                5_00L,
                CommonUtilities.euroToCents(TestUtils.bigDecimalValue(5))
        );
    }
}
