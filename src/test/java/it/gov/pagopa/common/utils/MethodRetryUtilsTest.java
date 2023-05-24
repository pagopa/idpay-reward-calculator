package it.gov.pagopa.common.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MethodRetryUtilsTest {

    @Test
    void test() {
        int[] effectiveAttempts = new int[]{0};
        int maxAttempts = 5;
        int expectedAttempts = maxAttempts+1;
        RuntimeException expectedException = new RuntimeException("DUMMY");

        try {
            MethodRetryUtils.exec("TEST", () -> {
                        effectiveAttempts[0]++;
                        throw expectedException;
                    },
                    maxAttempts);

        } catch (RuntimeException e) {
            Assertions.assertSame(expectedException, e);
        }

        Assertions.assertEquals(expectedAttempts, effectiveAttempts[0]);

    }
}
