package it.gov.pagopa.common.reactive.web.exception;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import it.gov.pagopa.common.utils.MemoryAppender;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ErrorManagerTest.TestController.class, ErrorManager.class})
@WebFluxTest
public class ErrorManagerTest {

    @SpyBean
    private TestController testController;

    @RestController
    @Slf4j
    static class TestController {
        @GetMapping("/test")
        Mono<String> testEndpoint(){
            return Mono.just("OK");
        }
    }

    @Autowired
    private WebTestClient webTestClient;
    private static MemoryAppender memoryAppender;

    @BeforeAll
    static void configureMemoryAppender(){
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        memoryAppender.start();
    }
    @BeforeEach
    void clearMemoryAppender(){
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ErrorManager.class.getName());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.reset();
    }


    @Test
    void handleExceptionClientExceptionNoBody() {
        Mockito.doThrow(new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, "NOTFOUND ClientExceptionNoBody"))
                .when(testController).testEndpoint();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().isEmpty();

        checkStackTraceSuppressedLog(memoryAppender,
                "A ClientExceptionNoBody occurred handling request GET /test \\([^)]+\\): HttpStatus 400 BAD_REQUEST - NOTFOUND ClientExceptionNoBody at it.gov.pagopa.common.reactive.web.exception.ErrorManagerTest\\$TestController.testEndpoint\\(ErrorManagerTest.java:[0-9]+\\)");
    }

    @Test
    void handleExceptionClientExceptionWithBody(){
        Mockito.doThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody"))
                .when(testController).testEndpoint();

        ErrorDTO errorClientExceptionWithBody= new ErrorDTO("Error","Error ClientExceptionWithBody");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBody);


        Mockito.doThrow(new ClientExceptionWithBody(HttpStatus.BAD_REQUEST, "Error","Error ClientExceptionWithBody", new Throwable()))
                .when(testController).testEndpoint();
        ErrorDTO errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable= new ErrorDTO("Error","Error ClientExceptionWithBody");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorDTO.class).isEqualTo(errorClientExceptionWithBodyWithStatusAndTitleAndMessageAndThrowable);
    }

    @Test
    void handleExceptionClientExceptionTest(){
        ErrorDTO expectedErrorClientException = new ErrorDTO("Error","Something gone wrong");

        Mockito.doThrow(ClientException.class)
                .when(testController).testEndpoint();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);

        checkStackTraceSuppressedLog(memoryAppender, "A ClientException occurred handling request GET /test \\([^)]+\\): HttpStatus null - null at UNKNOWN");
        memoryAppender.reset();

        Mockito.doThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus and message"))
                .when(testController).testEndpoint();
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);

        checkStackTraceSuppressedLog(memoryAppender, "A ClientException occurred handling request GET /test \\([^)]+\\): HttpStatus 400 BAD_REQUEST - ClientException with httpStatus and message at it.gov.pagopa.common.reactive.web.exception.ErrorManagerTest\\$TestController.testEndpoint\\(ErrorManagerTest.java:[0-9]+\\)");
        memoryAppender.reset();

        Mockito.doThrow(new ClientException(HttpStatus.BAD_REQUEST, "ClientException with httpStatus, message and throwable", new Throwable()))
                .when(testController).testEndpoint();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorClientException);

        checkLog(memoryAppender,
                "Something went wrong handling request GET /test \\([^)]+\\): HttpStatus 400 BAD_REQUEST - ClientException with httpStatus, message and throwable",
                "it.gov.pagopa.common.web.exception.ClientException: ClientException with httpStatus, message and throwable",
                "it.gov.pagopa.common.reactive.web.exception.ErrorManagerTest$TestController.testEndpoint"
        );
    }

    @Test
    void handleExceptionRuntimeException(){
        ErrorDTO expectedErrorDefault = new ErrorDTO("Error","Something gone wrong");

        Mockito.doThrow(RuntimeException.class)
                .when(testController).testEndpoint();
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDefault);
    }

    public static void checkStackTraceSuppressedLog(MemoryAppender memoryAppender, String expectedLoggedMessage) {
        String loggedMessage = memoryAppender.getLoggedEvents().get(0).getFormattedMessage();
        Assertions.assertTrue(Pattern.matches(expectedLoggedMessage, loggedMessage),
                "Unexpected logged message: " + loggedMessage);
    }

    public static void checkLog(MemoryAppender memoryAppender, String expectedLoggedMessageRegexp, String expectedLoggedExceptionMessage, String expectedLoggedExceptionOccurrencePosition) {
        ILoggingEvent loggedEvent = memoryAppender.getLoggedEvents().get(0);
        IThrowableProxy loggedException = loggedEvent.getThrowableProxy();
        StackTraceElementProxy loggedExceptionOccurrenceStackTrace = loggedException.getStackTraceElementProxyArray()[0];

        String loggedMessage = loggedEvent.getFormattedMessage();
        Assertions.assertTrue(Pattern.matches(expectedLoggedMessageRegexp,
                        loggedEvent.getFormattedMessage()),
                "Unexpected logged message: " + loggedMessage);

        Assertions.assertEquals(expectedLoggedExceptionMessage,
                loggedException.getClassName() + ": " + loggedException.getMessage());

        Assertions.assertEquals(expectedLoggedExceptionOccurrencePosition,
                loggedExceptionOccurrenceStackTrace.getStackTraceElement().getClassName() + "." + loggedExceptionOccurrenceStackTrace.getStackTraceElement().getMethodName());
    }
}