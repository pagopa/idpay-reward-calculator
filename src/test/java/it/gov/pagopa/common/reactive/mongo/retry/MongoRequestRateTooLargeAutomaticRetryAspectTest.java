package it.gov.pagopa.common.reactive.mongo.retry;

import it.gov.pagopa.common.reactive.web.ReactiveRequestContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class MongoRequestRateTooLargeAutomaticRetryAspectTest {

  @Mock
  private ProceedingJoinPoint pjpMock;

  private final int maxRetry = 1;

  private final String expectedResult = "OK";

  private final int[] counter= {0};

  private final Mono<String> expectedMonoResult = Mono.just(expectedResult);

  private final Flux<String> expectedFluxResult = Flux.just(expectedResult);

  @BeforeEach
  void init(){
    counter[0] = 0;
  }


  //region test batch mono
  @Test
  void testBatchMonoEnabled() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        false, true);

    checkRetryBehaviourMono(aspect, true);
  }

  @Test
  void testBatchMonoException() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        false, false);

    checkExceptionMono(aspect, true);
  }

  @Test
  void testBatchMonoDisabledApiEnabled() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        true, false);

    checkExceptionMono(aspect, true);
  }
  //endregion

  //region test batch flux
  @Test
  void testBatchFluxEnabled() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        false, true);

    checkRetryBehaviourFlux(aspect, true);
  }
  @Test
  void testBatchFluxException() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        false, false);

    checkExceptionFlux(aspect, true);
  }

  @Test
  void testBatchFluxDisabledApiEnabled() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        true, false);

    checkExceptionFlux(aspect, true);
  }
  //endregion

  //region test Api mono
  @Test
  void testApiMonoEnabled() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        true, false);

    checkRetryBehaviourMono(aspect, false);
  }

  @Test
  void testApiMonoException() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        false, false);

    checkExceptionMono(aspect, false);
  }

  @Test
  void testApiMonoDisabledBatchEnabled() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        false, true);

    checkExceptionMono(aspect, false);
  }
  //endregion

  //region test Api flux
  @Test
  void testApiFluxEnabled() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        true, false);

    checkRetryBehaviourFlux(aspect, false);
  }

  @Test
  void testApiFluxException() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        false, false);

    checkExceptionFlux(aspect, false);
  }

  @Test
  void testApiFluxDisabledBatchEnabled() throws Throwable {
    MongoRequestRateTooLargeAutomaticRetryAspect aspect = buildMongoRequestRateTooLargeAutomaticRetryAspect(
        false, true);

    checkExceptionFlux(aspect, false);
  }
  //endregion

  private MongoRequestRateTooLargeAutomaticRetryAspect buildMongoRequestRateTooLargeAutomaticRetryAspect(
      boolean enabledApi, boolean enabledBatch) {
    return new MongoRequestRateTooLargeAutomaticRetryAspect(
        enabledApi, maxRetry, 1000, enabledBatch, maxRetry, 1000);
  }

  private void configureRetryMockMono() throws Throwable {

    Mockito.doAnswer(i -> Mono.deferContextual(ctx -> {
      if (counter[0]++ < maxRetry){
        return Mono.error(MongoRequestRateTooLargeRetryerTest.buildRequestRateTooLargeMongodbException());
      }
      return expectedMonoResult;
    })).when(pjpMock).proceed();

    Signature signatureMock = Mockito.mock(Signature.class);
    Mockito.lenient().when(signatureMock.toShortString()).thenReturn("ClassName.jointPointName(..)");
    Mockito.lenient().when(pjpMock.getSignature()).thenReturn(signatureMock);
  }

  private void checkRetryBehaviourMono(MongoRequestRateTooLargeAutomaticRetryAspect aspect, boolean isBatch) throws Throwable {
    //Given
    configureRetryMockMono();

    //when
    Mono<?> mono = buildContextMono(aspect, isBatch);
    Object result = mono.block();

    //then
    Assertions.assertEquals(expectedResult, result);
    Assertions.assertEquals(maxRetry+1, counter[0]);
  }
  private void checkExceptionMono(MongoRequestRateTooLargeAutomaticRetryAspect aspect, boolean isBatch)
      throws Throwable {
    configureRetryMockMono();
    Mono<?> mono = buildContextMono(aspect, isBatch);
    UncategorizedMongoDbException uncategorizedMongoDbException = Assertions.assertThrows(UncategorizedMongoDbException.class, mono::block);
    Assertions.assertEquals( MongoRequestRateTooLargeRetryerTest.buildRequestRateTooLargeMongodbException().getMessage() ,uncategorizedMongoDbException.getMessage());
  }

  private Mono<?> buildContextMono(MongoRequestRateTooLargeAutomaticRetryAspect aspect, boolean isBatch)
      throws Throwable {
    Mono<?> mono = (Mono<?>) aspect.decorateMonoRepositoryMethods(pjpMock);
    if(!isBatch){
      mono = mono.contextWrite(ctx -> ctx.put(ReactiveRequestContextHolder.CONTEXT_KEY, Mockito.mock(
          ServerWebExchange.class)));
    }
    return mono;
  }

  private void configureRetryMockFlux() throws Throwable {

    Mockito.doAnswer(i -> Flux.deferContextual(ctx -> {
      if (counter[0]++ < maxRetry){
        return Flux.error(MongoRequestRateTooLargeRetryerTest.buildRequestRateTooLargeMongodbException());
      }
      return expectedFluxResult;
    })).when(pjpMock).proceed();

    Signature signatureMock = Mockito.mock(Signature.class);
    Mockito.lenient().when(signatureMock.toShortString()).thenReturn("ClassName.jointPointName(..)");
    Mockito.lenient().when(pjpMock.getSignature()).thenReturn(signatureMock);
  }

  private void checkRetryBehaviourFlux(MongoRequestRateTooLargeAutomaticRetryAspect aspect, boolean isBatch) throws Throwable {
    //Given
    configureRetryMockFlux();

    //when
    Flux<Object> flux = buildContextFlux(aspect, isBatch);
    Object result = flux.blockLast();

    //then
    Assertions.assertEquals(expectedResult, result);
    Assertions.assertEquals(maxRetry+1, counter[0]);
  }
  private void checkExceptionFlux(MongoRequestRateTooLargeAutomaticRetryAspect aspect, boolean isBatch)
      throws Throwable {
    configureRetryMockFlux();
    Flux<Object> flux = buildContextFlux(aspect, isBatch);
    UncategorizedMongoDbException uncategorizedMongoDbException = Assertions.assertThrows(UncategorizedMongoDbException.class, flux::blockLast);
    Assertions.assertEquals( MongoRequestRateTooLargeRetryerTest.buildRequestRateTooLargeMongodbException().getMessage() ,uncategorizedMongoDbException.getMessage());
  }

  private Flux<Object> buildContextFlux(MongoRequestRateTooLargeAutomaticRetryAspect aspect, boolean isBatch)
      throws Throwable {
    @SuppressWarnings("unchecked")
    Flux<Object> flux = (Flux<Object>) aspect.decorateFluxRepositoryMethods(pjpMock);
    if(!isBatch){
      flux = flux.contextWrite(ctx -> ctx.put(ReactiveRequestContextHolder.CONTEXT_KEY, Mockito.mock(
          ServerWebExchange.class)));
    }
    return flux;
  }

}
