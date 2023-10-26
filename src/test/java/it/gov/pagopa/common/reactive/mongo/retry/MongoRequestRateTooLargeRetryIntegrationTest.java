package it.gov.pagopa.common.reactive.mongo.retry;

import it.gov.pagopa.common.mongo.config.MongoConfig;
import it.gov.pagopa.common.reactive.mongo.DummySpringRepository;
import it.gov.pagopa.common.reactive.mongo.config.ReactiveMongoConfig;
import it.gov.pagopa.common.reactive.mongo.retry.exception.MongoRequestRateTooLargeRetryExpiredException;
import it.gov.pagopa.common.reactive.web.ReactiveRequestContextFilter;
import it.gov.pagopa.common.reactive.web.ReactiveRequestContextHolder;
import it.gov.pagopa.common.web.exception.ErrorManager;
import it.gov.pagopa.common.web.exception.MongoExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@TestPropertySource(
        properties = {
                "de.flapdoodle.mongodb.embedded.version=4.0.21",

                "spring.data.mongodb.database=idpay",
                "spring.data.mongodb.config.connectionPool.maxSize: 100",
                "spring.data.mongodb.config.connectionPool.minSize: 0",
                "spring.data.mongodb.config.connectionPool.maxWaitTimeMS: 120000",
                "spring.data.mongodb.config.connectionPool.maxConnectionLifeTimeMS: 0",
                "spring.data.mongodb.config.connectionPool.maxConnectionIdleTimeMS: 120000",
                "spring.data.mongodb.config.connectionPool.maxConnecting: 2",
        })
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ReactiveRequestContextFilter.class,
        MongoRequestRateTooLargeAutomaticRetryAspect.class,
        ErrorManager.class,
        MongoExceptionHandler.class,
        MongoConfig.class,
        ReactiveMongoConfig.class,

        MongoRequestRateTooLargeRetryIntegrationTest.TestController.class,
        MongoRequestRateTooLargeRetryIntegrationTest.TestRepository.class
})
@WebFluxTest
@AutoConfigureDataMongo
@EnableAutoConfiguration
class MongoRequestRateTooLargeRetryIntegrationTest {

    @Value("${mongo.request-rate-too-large.batch.max-retry:3}")
    private int maxRetry;
    @Value("${mongo.request-rate-too-large.batch.max-millis-elapsed:0}")
    private int maxMillisElapsed;

    private static final int API_RETRYABLE_MAX_RETRY = 5;

    @SpyBean
    private TestRepository testRepositorySpy;
    @Autowired
    private DummySpringRepository dummySpringRepository;

    @SpyBean
    private MongoRequestRateTooLargeAutomaticRetryAspect automaticRetryAspectSpy;

    private static int[] counter;

    @BeforeEach
    void init() {
        counter = new int[]{0};
    }

    @RestController
    @Slf4j
    static class TestController {

        @Autowired
        private TestRepository repository;

        @GetMapping("/testMono")
        Mono<String> testMonoEndpoint() {
            return ReactiveRequestContextHolder.getRequest()
                    .doOnNext(r -> {
                        System.out.println("OK");
                        Assertions.assertEquals("/testMono", r.getRequest().getURI().getPath());
                    })
                    .flatMap(x -> buildNestedRepositoryMonoMethodInvoke(repository));
        }

        @MongoRequestRateTooLargeApiRetryable(maxRetry = API_RETRYABLE_MAX_RETRY)
        @GetMapping("/testMonoRetryable")
        Mono<String> testMonoEndpointRetryable() {
            return ReactiveRequestContextHolder.getRequest()
                    .doOnNext(r -> {
                        System.out.println("OK");
                        Assertions.assertEquals("/testMonoRetryable", r.getRequest().getURI().getPath());
                    })
                    .flatMap(x -> buildNestedRepositoryMonoMethodInvoke(repository));
        }

        @GetMapping("/testFlux")
        Flux<LocalDateTime> testFluxEndpoint() {
            return ReactiveRequestContextHolder.getRequest()
                    .doOnNext(r -> {
                        System.out.println("OK");
                        Assertions.assertEquals("/testFlux", r.getRequest().getURI().getPath());
                    })
                    .flatMapMany(x -> buildNestedFluxChain(repository));
        }

        @MongoRequestRateTooLargeApiRetryable(maxRetry = API_RETRYABLE_MAX_RETRY)
        @GetMapping("/testFluxRetryable")
        Flux<LocalDateTime> testFluxEndpointRetryable() {
            return ReactiveRequestContextHolder.getRequest()
                    .doOnNext(r -> {
                        System.out.println("OK");
                        Assertions.assertEquals("/testFluxRetryable", r.getRequest().getURI().getPath());
                    })
                    .flatMapMany(x -> buildNestedFluxChain(repository));
        }

        static Mono<String> buildNestedRepositoryMonoMethodInvoke(TestRepository repository) {
            return Mono.just("")
                    .flatMap(x ->
                            Mono.delay(Duration.ofMillis(5))
                                    .flatMap(y -> repository.testMono())
                    );
        }

        static Flux<LocalDateTime> buildNestedFluxChain(TestRepository repository) {
            return Flux.just("")
                    .flatMap(x ->
                            Mono.delay(Duration.ofMillis(5))
                                    .flatMapMany(y -> repository.testFlux())
                    );
        }
    }

    @Service
    static class TestRepository {
        public Mono<String> testMono() {
            return Mono.defer(() -> {
                counter[0]++;
                return Mono.error(MongoRequestRateTooLargeRetryerTest.buildRequestRateTooLargeMongodbException_whenReading());
            });
        }

        public Flux<LocalDateTime> testFlux() {
            return Flux.defer(() -> {
                counter[0]++;
                return Flux.error(MongoRequestRateTooLargeRetryerTest.buildRequestRateTooLargeMongodbException_whenReading());
            });
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testController_MonoMethod() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/testMono").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody().json("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"TOO_MANY_REQUESTS\"}");

        Assertions.assertEquals(1, counter[0]);
    }

    @Test
    void testController_MonoMethodRetryable() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/testMonoRetryable").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody().json("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"TOO_MANY_REQUESTS\"}");

        Assertions.assertEquals(API_RETRYABLE_MAX_RETRY + 1, counter[0]);
    }

    @Test
    void testController_FluxMethod() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/testFlux").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody().json("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"TOO_MANY_REQUESTS\"}");

        Assertions.assertEquals(1, counter[0]);
    }

    @Test
    void testController_FluxMethodRetryable() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/testFluxRetryable").build())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody().json("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"TOO_MANY_REQUESTS\"}");

        Assertions.assertEquals(API_RETRYABLE_MAX_RETRY + 1, counter[0]);
    }

    @Test
    void testNoController_MonoMethod() {
        testNoController(TestController.buildNestedRepositoryMonoMethodInvoke(testRepositorySpy));
    }

    @Test
    void testNoController_FluxMethod() {
        testNoController(TestController.buildNestedFluxChain(testRepositorySpy).collectList());
    }

    private void testNoController(Mono<?> mono) {
        try {
            mono.block();
            Assertions.fail("Expected exception");
        } catch (MongoRequestRateTooLargeRetryExpiredException e) {
            Assertions.assertEquals(maxRetry + 1, e.getCounter());
            Assertions.assertEquals(maxRetry, e.getMaxRetry());
            Assertions.assertEquals(maxMillisElapsed, e.getMaxMillisElapsed());
            Assertions.assertTrue(e.getMillisElapsed() > 0);
        }

        Assertions.assertEquals(maxRetry + 1, counter[0]);
    }

    @Test
    void testSpringRepositoryInterceptor() throws Throwable {
        // When
        dummySpringRepository.findByIdOrderById("ID");
        dummySpringRepository.findByIdOrderByIdDesc("ID2");

        // Then
        Mockito.verify(automaticRetryAspectSpy).decorateMonoRepositoryMethods(Mockito.argThat(i -> i.getArgs()[0].equals("ID")));
        Mockito.verify(automaticRetryAspectSpy).decorateFluxRepositoryMethods(Mockito.argThat(i -> i.getArgs()[0].equals("ID2")));
    }
}