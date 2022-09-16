package it.gov.pagopa.reward.test.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.reward.config.JsonConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {
    private TestUtils() {
    }

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
    }

    /**
     * applications's objectMapper
     */
    public static ObjectMapper objectMapper = new JsonConfig().objectMapper();

    /**
     * It will assert not null on all o's fields
     */
    public static void checkNotNullFields(Object o, String... excludedFields) {
        Set<String> excludedFieldsSet = new HashSet<>(Arrays.asList(excludedFields));
        org.springframework.util.ReflectionUtils.doWithFields(o.getClass(),
                f -> {
                    f.setAccessible(true);
                    Assertions.assertNotNull(f.get(o), "The field %s of the input object of type %s is null!".formatted(f.getName(), o.getClass()));
                },
                f -> !excludedFieldsSet.contains(f.getName()));

    }

    /** it will create a BigDecimal with scale2 */
    public static BigDecimal bigDecimalValue(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * It will assert if 2 BigDecimal are equals, ignoring scale
     */
    public static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), "Expected: %s, Obtained: %s".formatted(expected, actual));
    }

    /**
     * To serialize an object as a JSON handling Exception
     */
    public static String jsonSerializer(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * To read {@link org.apache.kafka.common.header.Header} value
     */
    public static String getHeaderValue(ConsumerRecord<String, String> errorMessage, String errorMsgHeaderSrcServer) {
        return new String(errorMessage.headers().lastHeader(errorMsgHeaderSrcServer).value());
    }

}
