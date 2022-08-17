package it.gov.pagopa.reward.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.messaging.Message;

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
}
