package it.gov.pagopa.common.reactive.web.exception;


import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.utils.MemoryAppender;
import it.gov.pagopa.common.web.exception.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ServiceWithPayloadExceptionHandler.class, ServiceWithPayloadExceptionHandlerTest.TestController.class})
@WebFluxTest
class ServiceWithPayloadExceptionHandlerTest {
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
    void clearAndAppendMemoryAppender(){
        memoryAppender.reset();

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ErrorManager.class.getName());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
    }

    @RestController
    @Slf4j
    static class TestController {
        @GetMapping("/test")
        Mono<String> test() {
            throw new ServiceWithPayloadException(
                    "DUMMY_CODE",
                    "DUMMY_MESSAGE",
                    new ErrorPayloadTest("RESPONSE",0));
        }

        @GetMapping("/test/customBody")
        Mono<String> testCustomBody() {
            throw new ServiceWithPayloadException(
                    "DUMMY_CODE",
                    "DUMMY_MESSAGE",
                    new ErrorPayloadTest("RESPONSE",0),
                    true,
                    null);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ErrorPayloadTest implements ServiceExceptionPayload {
        private String stringCode;
        private long longCode;
    }

    @Test
    void testSimpleException() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().json("{\"stringCode\":\"RESPONSE\",\"longCode\":0}", false);

        ErrorManagerTest.checkStackTraceSuppressedLog(memoryAppender, "Something went wrong handling request GET /test \\([^)]+\\): HttpStatus 500 INTERNAL_SERVER_ERROR - DUMMY_CODE: DUMMY_MESSAGE");
    }

    @Test
    void testCustomBodyException() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test/customBody").build())
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().json("{\"stringCode\":\"RESPONSE\",\"longCode\":0}", false);

        ErrorManagerTest.checkLog(memoryAppender,
                "Something went wrong handling request GET /test/customBody \\([^)]+\\): HttpStatus 500 INTERNAL_SERVER_ERROR - DUMMY_CODE: DUMMY_MESSAGE",
                "it.gov.pagopa.common.web.exception.ClientExceptionWithBody: DUMMY_MESSAGE",
                "it.gov.pagopa.common.web.exception.ServiceWithPayloadExceptionHandler.transcodeException"

        );
    }
}
