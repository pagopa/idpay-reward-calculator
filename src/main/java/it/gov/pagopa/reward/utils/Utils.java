package it.gov.pagopa.reward.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.TransactionDTO;
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

    private static final String PAYLOAD_FIELD_USER_ID = "\"%s\"".formatted(TransactionDTO.Fields.userId);
    /** It will read userId field from {@link it.gov.pagopa.reward.dto.TransactionDTO} payload */
    public static String readUserId(String payload) {
        int userIdIndex = payload.indexOf(PAYLOAD_FIELD_USER_ID);
        if(userIdIndex>-1){
            String afterUserId = payload.substring(userIdIndex+8);
            final int afterOpeningQuote = afterUserId.indexOf('"') + 1;
            return afterUserId.substring(afterOpeningQuote, afterUserId.indexOf('"', afterOpeningQuote));
        }
        return null;
    }
}
