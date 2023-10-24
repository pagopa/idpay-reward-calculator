package it.gov.pagopa.common.reactive.mongo.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ch.qos.logback.classic.LoggerContext;
import com.mongodb.MongoQueryException;
import com.mongodb.ServerAddress;
import it.gov.pagopa.common.reactive.mongo.retry.exception.MongoRequestRateTooLargeRetryExpiredException;
import it.gov.pagopa.common.utils.MemoryAppender;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


class MongoRequestRateTooLargeRetryerTest {

    private static final int REQUEST_RATE_TOO_LARGE_MAX_RETRY = 10;
    public static final int REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED = 1000;
    private MemoryAppender memoryAppender;

    @BeforeEach
    public void setup() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                MongoRequestRateTooLargeRetryer.class.getName());
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    //region Mono ops
    @Test
    void testMonoWithRetryMaxRetry_resultOk(){
        int[] counter = {0};

        Mono<Object> testPublisher = Mono.fromSupplier(() -> counter[0]++)
                .map(x -> {
                    if (counter[0] <= REQUEST_RATE_TOO_LARGE_MAX_RETRY) {
                        return throwRequestRateTooLargeMongodbException();
                    }
                    return "OK";
                });

        assertLogForMaxRetryTest(MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher,
                REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0).block(), counter[0]);
    }

    @Test
    void testMonoWithRetryMaxMillisElapsed_resultOk(){
        int[] counter = {0};
        long startTime = System.currentTimeMillis();

        Mono<Object> testPublisher = Mono.fromSupplier(() -> counter[0]++)
                .map(x -> {
                    if (System.currentTimeMillis() - startTime < (REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED / 2)) {
                        return throwRequestRateTooLargeMongodbException();
                    }
                    return "OK";
                });

        assertLogForMaxMillisElapsedTest(MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher,
                0, REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED).block());

    }

    @Test
    void testMonoWithRetryMaxRetry_resultKo(){
        int[] counter = {0};

        Mono<Object> testPublisher = Mono.fromSupplier(() -> counter[0]++)
                .map(x -> throwRequestRateTooLargeMongodbException());
        Mono<Object> mono = MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher
                , REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0);

        try {
            mono.block();
            fail();
        } catch (MongoRequestRateTooLargeRetryExpiredException e) {
            assertCatchMongoRequestRateTooLargeRetryExpiredException(e, counter[0], REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0);
        }

        assertLogForMaxRetryTest(null, counter[0]);
    }

    @Test
    void testMonoWithRetryMaxMillisElapsed_resultKo(){
        int[] counter = {0};

        Mono<Object> testPublisher = Mono.fromSupplier(() -> counter[0]++)
                .map(x -> throwRequestRateTooLargeMongodbException());
        Mono<Object> mono = MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher
                , 0, REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED);

        try {
            mono.block();
            fail();
        } catch (MongoRequestRateTooLargeRetryExpiredException e) {
            assertCatchMongoRequestRateTooLargeRetryExpiredException(e, counter[0], 0, REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED);
        }

        assertLogForMaxMillisElapsedTest(null);
    }

    @Test
    void testMonoUncategorizedMongoDbExceptionNotRequestRateTooLarge() {
        int[] counter = {0};
        UncategorizedMongoDbException exception = new UncategorizedMongoDbException(
                "not Request Rate Too Large Exception", new Throwable());

        Mono<Object> testPublisher = Mono.fromSupplier(() -> counter[0]++)
                .map(x -> {
                    throw new UncategorizedMongoDbException("not Request Rate Too Large Exception", new Throwable());
                });
        Mono<Object> mono = MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher
                , REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0);

        try {
            mono.block();
            fail();
        } catch (Exception e) {
            assertEquals(exception.getMessage(), e.getMessage());
        }
    }

    @Test
    void testMonoRequestRateTooLargeRetryAfterMsNull() {
        long[] counter = {0};
        UncategorizedMongoDbException expectedException = new UncategorizedMongoDbException(
                "TooManyRequests", new Throwable());
        Mono<Object> testPublisher = Mono.fromSupplier(() -> counter[0]++)
                .map(x -> {
                    if (counter[0] <= REQUEST_RATE_TOO_LARGE_MAX_RETRY) {
                        throw expectedException;
                    }
                    return "OK";
                });
        MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher
                , REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0).block();

        assertLogForRequestRateTooLargeRetryAfterMsNull();

    }
//endregion

    //region Flux ops
    @Test
    void testFluxWithRetryMaxRetry_resultOk(){
        int[] counter = {0};

        Flux<Object> testPublisher = Flux.defer(() -> Flux.just(counter[0]++))
                .map(x -> {
                    if (counter[0] <= REQUEST_RATE_TOO_LARGE_MAX_RETRY) {
                        return throwRequestRateTooLargeMongodbException();
                    }
                    return "OK";
                });

        assertLogForMaxRetryTest(MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher,
                REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0).blockLast(), counter[0]);
    }


    @Test
    void testFluxWithRetryMaxMillisElapsed_resultOk(){
        int[] counter = {0};
        long startTime = System.currentTimeMillis();

        Flux<Object> testPublisher = Flux.defer(() -> Flux.just(counter[0]++))
                .map(x -> {
                    if (System.currentTimeMillis() - startTime < (REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED / 2)) {
                        return throwRequestRateTooLargeMongodbException();
                    }
                    return "OK";
                });

        assertLogForMaxMillisElapsedTest(MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher,
                0, REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED).blockLast());

    }

    @Test
    void testFluxWithRetryMaxRetry_resultKo(){
        int[] counter = {0};

        Flux<Object> testPublisher = Flux.defer(() -> Flux.just(counter[0]++))
                .map(x -> throwRequestRateTooLargeMongodbException());
        Flux<Object> flux = MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher
                , REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0);

        try {
            flux.blockLast();
            fail();
        } catch (MongoRequestRateTooLargeRetryExpiredException e) {
            assertCatchMongoRequestRateTooLargeRetryExpiredException(e, counter[0], REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0);
        }

        assertLogForMaxRetryTest(null, counter[0]);
    }

    @Test
    void testFluxWithRetryMaxMillisElapsed_resultKo(){
        int[] counter = {0};

        Flux<Object> testPublisher = Flux.defer(() -> Flux.just(counter[0]++))
                .map(x -> throwRequestRateTooLargeMongodbException());
        Flux<Object> flux = MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher
                , 0, REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED);

        try {
            flux.blockLast();
            fail();
        } catch (MongoRequestRateTooLargeRetryExpiredException e) {
            assertCatchMongoRequestRateTooLargeRetryExpiredException(e, counter[0], 0, REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED);
        }

        assertLogForMaxMillisElapsedTest(null);
    }

    @Test
    void testFluxUncategorizedMongoDbExceptionNotRequestRateTooLarge() {
        int[] counter = {0};
        UncategorizedMongoDbException exception = new UncategorizedMongoDbException(
                "not Request Rate Too Large Exception", new Throwable());

        Flux<Object> testPublisher = Flux.defer(() -> Flux.just(counter[0]++))
                .map(x -> {
                    throw new UncategorizedMongoDbException("not Request Rate Too Large Exception", new Throwable());
                });
        Flux<Object> flux = MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher
                , REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0);

        try {
            flux.blockLast();
            fail();
        } catch (Exception e) {
            assertEquals(exception.getMessage(), e.getMessage());
        }
    }

    @Test
    void testFluxRequestRateTooLargeRetryAfterMsNull() {
        long[] counter = {0};
        UncategorizedMongoDbException expectedException = new UncategorizedMongoDbException(
                "TooManyRequests", new Throwable());
        Flux<Object> testPublisher = Flux.defer(() -> Flux.just(counter[0]++))
                .map(x -> {
                    if (counter[0] <= REQUEST_RATE_TOO_LARGE_MAX_RETRY) {
                        throw expectedException;
                    }
                    return "OK";
                });
        MongoRequestRateTooLargeRetryer.withRetry("FLOWNAME", testPublisher
                , REQUEST_RATE_TOO_LARGE_MAX_RETRY, 0).blockLast();

        assertLogForRequestRateTooLargeRetryAfterMsNull();

    }
//endregion

    public static UncategorizedMongoDbException buildRequestRateTooLargeMongodbException() {
        String mongoFullErrorResponse = """
        {"ok": 0.0, "errmsg": "Error=16500, RetryAfterMs=34,\s
        Details='Response status code does not indicate success: TooManyRequests (429) Substatus: 3200 ActivityId: 46ba3855-bc3b-4670-8609-17e1c2c87778 Reason:\s
        (\\r\\nErrors : [\\r\\n \\"Request rate is large. More Request Units may be needed, so no changes were made. Please retry this request later. Learn more:
         http://aka.ms/cosmosdb-error-429\\"\\r\\n]\\r\\n) ", "code": 16500, "codeName": "RequestRateTooLarge"}
        """;

        MongoQueryException mongoQueryException = new MongoQueryException(
                BsonDocument.parse(mongoFullErrorResponse), new ServerAddress());
        return new UncategorizedMongoDbException(mongoQueryException.getMessage(), mongoQueryException);
    }

    public static UncategorizedMongoDbException throwRequestRateTooLargeMongodbException() {

        throw buildRequestRateTooLargeMongodbException();
    }

    private void assertLogMessage(String expectedMessage, long maxRetryOrMaxMillisElapsed) {
        for (int i = 0; i < memoryAppender.getLoggedEvents().size(); i++) {

            String logMessage = memoryAppender.getLoggedEvents().get(i).getFormattedMessage();
            assertTrue(
                    logMessage.matches(expectedMessage.formatted(i + 1, maxRetryOrMaxMillisElapsed)),
                    "Logged message:\n   %s\nexpected format:\n   %s".formatted(logMessage, expectedMessage)
            );
        }
    }

    private void assertLogForRequestRateTooLargeRetryAfterMsNull() {
        String expectedMessage = "\\[REQUEST_RATE_TOO_LARGE_RETRY]\\[FLOWNAME] Retrying for RequestRateTooLargeException: attempt %d of %d after .*";
        assertEquals(REQUEST_RATE_TOO_LARGE_MAX_RETRY, memoryAppender.getLoggedEvents().size());
        assertLogMessage(expectedMessage, REQUEST_RATE_TOO_LARGE_MAX_RETRY);
    }

    private void assertLogForMaxMillisElapsedTest(Object testPublisher) {
        if(testPublisher!=null) {
            assertEquals("OK", testPublisher);
        }
        String message = "\\[REQUEST_RATE_TOO_LARGE_RETRY]\\[FLOWNAME] Retrying after 34 ms due to RequestRateTooLargeException: attempt %d of \\d+ after \\d+ ms of max %d ms";
        assertLogMessage(message, REQUEST_RATE_TOO_LARGE_MAX_MILLIS_ELAPSED);
    }

    private void assertLogForMaxRetryTest(Object testPublisher, int counter) {
        if(testPublisher!=null) {
            assertEquals("OK", testPublisher);
        }
        assertEquals(REQUEST_RATE_TOO_LARGE_MAX_RETRY + 1, counter);
        String expectedMessage = "\\[REQUEST_RATE_TOO_LARGE_RETRY]\\[FLOWNAME] Retrying after 34 ms due to RequestRateTooLargeException: attempt %d of %d after .*";
        assertLogMessage(expectedMessage, REQUEST_RATE_TOO_LARGE_MAX_RETRY);
    }

    private void assertCatchMongoRequestRateTooLargeRetryExpiredException(MongoRequestRateTooLargeRetryExpiredException e, int counter,
                                                                          int maxRetry, int maxMillisElapsed){
        if(maxRetry > 0){
            assertEquals(maxRetry, e.getMaxRetry());
            assertEquals(maxRetry + 1, e.getCounter());
            assertEquals(0, e.getMaxMillisElapsed());
        }else {
            assertEquals(0, e.getMaxRetry());
            assertEquals(counter, e.getCounter());
            assertEquals(maxMillisElapsed, e.getMaxMillisElapsed());
            assertTrue(maxMillisElapsed < e.getMillisElapsed());
        }
    }
}