package it.gov.pagopa.common.kafka.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
class ErrorNotifierServiceTest {

    private static final String APPLICATION_NAME = "APPNAME";

    public static final String SRC_TYPE = "SRCTYPE";
    public static final String SRC_SERVER = "SRCSERVER";
    public static final String SRC_TOPIC = "SRCTOPIC";
    public static final String GROUP = "GROUP";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final RuntimeException EXCEPTION = new RuntimeException(new RuntimeException("DUMMYCAUSE"));
    public static final RuntimeException EXCEPTIONNOTROOT = new RuntimeException();

    public static final String PAYLOAD = "PAYLOAD";
    public static final String HEADER_NAME = "KEY";
    public static final String HEADER_VALUE = "VALUE";

    public static final Message<String> MESSAGE = MessageBuilder.withPayload(PAYLOAD).setHeader(HEADER_NAME, HEADER_VALUE).build();

    @Mock
    private ErrorPublisher errorPublisherMock;

    private ErrorNotifierService service;

    @BeforeAll
    static void setLogLevel(){
        ((Logger) LoggerFactory.getLogger(ErrorNotifierServiceImpl.class)).setLevel(Level.WARN);
    }

    @BeforeEach
    void init() {
        service = new ErrorNotifierServiceImpl(APPLICATION_NAME, errorPublisherMock);
    }

    @Test
    void test_retryable_resendApplication_exception_NOkey_returnTrue() {
        test(
                MESSAGE,
                true,
                true,
                true,
                null,
                EXCEPTION
        );
    }

    @Test
    void test_NOretryable_NOresendApplication_exceptionNOroot_key_returnFalse() {
        String keyValue = "KEYVALUE";

        test(
                MessageBuilder.fromMessage(MESSAGE).setHeader(KafkaHeaders.RECEIVED_KEY, keyValue.getBytes(StandardCharsets.UTF_8)).build(),
                false,
                false,
                false,
                keyValue,
                EXCEPTIONNOTROOT
        );
    }

    private void test(Message<?> message, boolean expectedResult, boolean expectedRetryable, boolean expectedResend, String expectedKeyValue, Throwable expectedException) {
        // Given
        Mockito.when(errorPublisherMock.send(Mockito.argThat(m -> assertErrorMessage(m, expectedRetryable, expectedResend, expectedKeyValue, expectedException)
        ))).thenReturn(expectedResult);

        // When
        boolean result = service.notify(SRC_TYPE, SRC_SERVER, SRC_TOPIC, GROUP, message, DESCRIPTION, expectedRetryable, expectedResend, expectedException);

        // Then
        Assertions.assertEquals(expectedResult, result);
    }

    private static boolean assertErrorMessage(Message<?> m, boolean expectedRetryable, boolean expectedResend, String expectedKeyValue, Throwable expectedException) {
        Assertions.assertEquals(PAYLOAD, m.getPayload());
        Assertions.assertEquals(HEADER_VALUE, m.getHeaders().get(HEADER_NAME, String.class));

        Assertions.assertEquals(SRC_TYPE, m.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_SRC_TYPE));
        Assertions.assertEquals(SRC_SERVER, m.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_SRC_SERVER));
        Assertions.assertEquals(SRC_TOPIC, m.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_SRC_TOPIC));

        Assertions.assertEquals(DESCRIPTION, m.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_DESCRIPTION));

        Assertions.assertEquals(expectedRetryable, m.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_RETRYABLE));

        Assertions.assertEquals(ExceptionUtils.getStackTrace(expectedException), m.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_STACKTRACE));
        Assertions.assertEquals((expectedException == EXCEPTION ? ExceptionUtils.getRootCause(EXCEPTION) : EXCEPTIONNOTROOT).getClass().getName(), m.getHeaders().get("rootCauseClass"));
        Assertions.assertEquals((expectedException == EXCEPTION ? ExceptionUtils.getRootCause(EXCEPTION) : EXCEPTIONNOTROOT).getMessage(), m.getHeaders().get("rootCauseMessage"));
        Assertions.assertEquals(expectedException == EXCEPTION ? EXCEPTION.getCause().getClass().getName() : null, m.getHeaders().get("causeClass"));
        Assertions.assertEquals(expectedException == EXCEPTION ? EXCEPTION.getCause().getMessage() : null, m.getHeaders().get("causeMessage"));

        Assertions.assertEquals(expectedKeyValue, m.getHeaders().get(KafkaHeaders.KEY));

        Assertions.assertEquals(expectedResend ? APPLICATION_NAME : null, m.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME));
        Assertions.assertEquals(expectedResend ? GROUP : null, m.getHeaders().get(KafkaConstants.ERROR_MSG_HEADER_GROUP));

        return true;
    }
}
