package it.gov.pagopa.common.reactive.web.exception;


import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.utils.MemoryAppender;
import it.gov.pagopa.common.web.exception.ErrorManager;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionHandler;
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
@ContextConfiguration(classes = {ServiceExceptionHandler.class, ServiceExceptionHandlerTest.TestController.class, ErrorManager.class})
@WebFluxTest
class ServiceExceptionHandlerTest {
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
            throw new ServiceException("DUMMY_CODE", "DUMMY_MESSAGE");
        }

        @GetMapping("/test/customBody")
        Mono<String> testCustomBody() {
            throw new ServiceException("DUMMY_CODE", "DUMMY_MESSAGE", true, null);
        }
    }

    @Test
    void testSimpleException() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test").build())
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().json("{\"code\":\"DUMMY_CODE\",\"message\":\"DUMMY_MESSAGE\"}", false);

        ErrorManagerTest.checkStackTraceSuppressedLog(memoryAppender, "A ServiceException occurred handling request GET /test \\([^)]+\\): HttpStatus 500 INTERNAL_SERVER_ERROR - DUMMY_CODE: DUMMY_MESSAGE at it.gov.pagopa.common.reactive.web.exception.ServiceExceptionHandlerTest\\$TestController.test\\(ServiceExceptionHandlerTest.java:[0-9]+\\)");
    }

    @Test
    void testCustomBodyException() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/test/customBody").build())
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().json("{\"code\":\"DUMMY_CODE\",\"message\":\"DUMMY_MESSAGE\"}", false);

        ErrorManagerTest.checkLog(memoryAppender,
                "Something went wrong handling request GET /test/customBody \\([^)]+\\): HttpStatus 500 INTERNAL_SERVER_ERROR - DUMMY_CODE: DUMMY_MESSAGE",
                "it.gov.pagopa.common.web.exception.ServiceException: DUMMY_MESSAGE",
                "it.gov.pagopa.common.reactive.web.exception.ServiceExceptionHandlerTest$TestController.testCustomBody"

        );
    }
}
