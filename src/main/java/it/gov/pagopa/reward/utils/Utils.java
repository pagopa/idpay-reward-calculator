package it.gov.pagopa.reward.utils;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;

public final class Utils {

    private Utils(){}

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
}
