package it.gov.pagopa.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.messaging.Message;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.function.Consumer;

public class CommonUtilities {
    private CommonUtilities() {}

    public static final DecimalFormatSymbols decimalFormatterSymbols = new DecimalFormatSymbols();
    public static final DecimalFormat decimalFormatter;

    static {
        decimalFormatterSymbols.setDecimalSeparator(',');
        decimalFormatter = new DecimalFormat("0.00", CommonUtilities.decimalFormatterSymbols);
    }

    /** It will try to deserialize a message, eventually notifying the error  */
    public static <T> T deserializeMessage(Message<?> message, ObjectReader objectReader, Consumer<Throwable> onError) {
        try {
            String payload = readMessagePayload(message);
            return objectReader.readValue(payload);
        } catch (JsonProcessingException e) {
            onError.accept(e);
            return null;
        }
    }

    /** It will read message payload checking if it's a byte[] or String */
    public static String readMessagePayload(Message<?> message) {
        String payload;
        if(message.getPayload() instanceof byte[] bytes){
            payload=new String(bytes);
        } else {
            payload= message.getPayload().toString();
        }
        return payload;
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

    /** To format as String a Cents into euro */
    public static String centsToEuroString(Long cents) {
        return decimalFormatter.format(centsToEuro(cents));
    }

    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    /** To convert euro into cents */
    public static Long euroToCents(BigDecimal euro){
        return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
    }
}
