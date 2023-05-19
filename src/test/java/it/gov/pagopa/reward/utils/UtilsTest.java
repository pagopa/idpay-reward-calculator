package it.gov.pagopa.reward.utils;

import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

class UtilsTest {

    @Test
    void readUserIdPerformanceTest(){
        List<String> trxs = new ArrayList<>(IntStream.range(0, 1000)
                .mapToObj(TransactionDTOFaker::mockInstance)
                .map(TestUtils::jsonSerializer)
                .toList());

        trxs.add("\"userId\"   :   \"USERID_CUSTOM_0\"   ");
        trxs.add("\"userId\":   \"USERID_CUSTOM_1\"   ");
        trxs.add("\"userId\"   :\"USERID_CUSTOM_2\"   ");

        long regexpStartTime = System.currentTimeMillis();
        final List<String> regexpResult = trxs.stream().map(this::readUserIdUsingRegexp).toList();
        long regexpEndTime = System.currentTimeMillis();

        long indexOfStartTime = System.currentTimeMillis();
        final List<String> indexOfResult = trxs.stream().map(Utils::readUserId).toList();
        long indexOfEndTime = System.currentTimeMillis();

        System.out.printf(
                """
                        regexp time %d
                        indexOf time %d
                        %n""", regexpEndTime-regexpStartTime,
                indexOfEndTime-indexOfStartTime
        );

        Assertions.assertEquals(regexpResult, indexOfResult);
    }


    private final Pattern userIdPatternMatch = Pattern.compile("\"userId\"\s*:\s*\"([^\"]*)\"");
    private String readUserIdUsingRegexp(String payload) {
        final Matcher matcher = userIdPatternMatch.matcher(payload);
        return matcher.find() ? matcher.group(1) : "";
    }
}
