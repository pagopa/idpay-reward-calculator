package it.gov.pagopa.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class CommonUtilities {
    private CommonUtilities() {}

    /** It will try to deserialize a message, eventually notifying the error  */
    public static <T> T deserializeMessage(Message<String> message, ObjectReader objectReader, Consumer<Throwable> onError) {
        try {
            return objectReader.readValue(message.getPayload());
        } catch (JsonProcessingException e) {
            onError.accept(e);
            return null;
        }
    }

    /** To read Message header value */
    @SuppressWarnings("unchecked")
    public static <T> T getHeaderValue(Message<?> message, String headerName) {
        return  (T)message.getHeaders().get(headerName);
    }

    /** To read {@link org.apache.kafka.common.header.Header} value */
    public static String getByteArrayHeaderValue(Message<String> message, String headerName) {
        byte[] headerValue = message.getHeaders().get(headerName, byte[].class);
        return headerValue!=null? new String(headerValue, StandardCharsets.UTF_8) : null;
    }

    /** To convert cents into euro */
    public static BigDecimal centsToEuro(Long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN);
    }

    /** To convert euro into cents */
    public static Long euroToCents(BigDecimal euro) {
        return euro.longValue() * 100L;
    }
}
