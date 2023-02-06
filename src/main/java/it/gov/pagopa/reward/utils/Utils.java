package it.gov.pagopa.reward.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class Utils {

    private Utils(){}

    /** It will try to deserialize a message, eventually notifying the error  */
    public static <T> T deserializeMessage(Message<String> message, ObjectReader objectReader, Consumer<Throwable> onError) {
        try {
            return objectReader.readValue(message.getPayload());
        } catch (JsonProcessingException e) {
            onError.accept(e);
            return null;
        }
    }

    private static final String PAYLOAD_FIELD_USER_ID = "\"%s\"".formatted(TransactionDTO.Fields.userId);
    /** It will read userId field from {@link TransactionDTO} payload */
    public static String readUserId(String payload) {
        int userIdIndex = payload.indexOf(PAYLOAD_FIELD_USER_ID);
        if(userIdIndex>-1){
            String afterUserId = payload.substring(userIdIndex+8);
            final int afterOpeningQuote = afterUserId.indexOf('"') + 1;
            return afterUserId.substring(afterOpeningQuote, afterUserId.indexOf('"', afterOpeningQuote));
        }
        return null;
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
