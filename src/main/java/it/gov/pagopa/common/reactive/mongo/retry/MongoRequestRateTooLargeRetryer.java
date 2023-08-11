package it.gov.pagopa.common.reactive.mongo.retry;

import it.gov.pagopa.common.reactive.mongo.retry.exception.MongoRequestRateTooLargeRetryExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.Retry.RetrySignal;
import reactor.util.retry.RetrySpec;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MongoRequestRateTooLargeRetryer {

  private MongoRequestRateTooLargeRetryer() {
  }

  private static final Pattern RETRY_AFTER_MS_PATTERN = Pattern.compile("RetryAfterMs=(\\d+)");

  public static <T> Mono<T> withRetry(Mono<T> publisher, long maxRetry, long maxMillisElapsed){
    return publisher.retryWhen( buildRetry(maxRetry, maxMillisElapsed, System.currentTimeMillis()));
  }

  public static <T> Flux<T> withRetry(Flux<T> publisher, long maxRetry, long maxMillisElapsed){
    return publisher.retryWhen( buildRetry(maxRetry, maxMillisElapsed, System.currentTimeMillis()));
  }

  private static Retry buildRetry(long maxRetry, long maxMillisElapsed, long startTime) {
    Long[] retryAfterMs = {null};
    return buildBaseRetry(maxRetry)
        .filter(MongoRequestRateTooLargeRetryer::isRequestRateTooLargeException)
        .doBeforeRetryAsync(e -> {
          retryAfterMs[0] = getRetryAfterMs(e.failure());
          long millisElapsed = System.currentTimeMillis() - startTime;
          final long counter = e.totalRetries() + 1;

          if(retryAfterMs[0] != null){
            millisElapsed+=retryAfterMs[0];
          }

          if (maxMillisElapsed > 0 && millisElapsed > maxMillisElapsed){
            return Mono.error(buildMongoRequestRateTooLargeRetryExpiredException(maxRetry, e,
                maxMillisElapsed, millisElapsed, retryAfterMs[0]));
          }

          if (retryAfterMs[0] != null) {
            log.info(
                "[REQUEST_RATE_TOO_LARGE_RETRY] Retrying after {} ms due to RequestRateTooLargeException: attempt {} of {} after {} ms of max {} ms",
                retryAfterMs[0], counter, maxRetry, millisElapsed, maxMillisElapsed);
            return Mono.delay(Duration.ofMillis(retryAfterMs[0])).then();
          }else {
            log.info(
                "[REQUEST_RATE_TOO_LARGE_RETRY] Retrying for RequestRateTooLargeException: attempt {} of {} after {} ms of max {} ms",
                counter, maxRetry, millisElapsed, maxMillisElapsed);
            return Mono.empty();
          }
        })
        .onRetryExhaustedThrow((r, e) -> buildMongoRequestRateTooLargeRetryExpiredException(
            maxRetry, e, maxMillisElapsed,
            System.currentTimeMillis() - startTime, retryAfterMs[0]));
  }


  @NotNull
  private static MongoRequestRateTooLargeRetryExpiredException buildMongoRequestRateTooLargeRetryExpiredException(
      long maxRetry, RetrySignal e, long maxMillisElapsed, long startTime, Long retryAfterMs) {
    return new MongoRequestRateTooLargeRetryExpiredException(maxRetry, e.totalRetries() + 1,
        maxMillisElapsed, startTime, retryAfterMs, e.failure());
  }

  private static RetrySpec buildBaseRetry(long maxRetry) {
    return maxRetry <= 0
        ? Retry.indefinitely()
        : Retry.max(maxRetry);
  }


  public static Long getRetryAfterMs(Throwable ex) {
    Matcher matcher = RETRY_AFTER_MS_PATTERN.matcher(ex.getMessage());
    if (ex instanceof DataAccessException && matcher.find()) {
      return Long.parseLong(matcher.group(1));
    }
    return null;
  }

  public static boolean isRequestRateTooLargeException(Throwable ex) {
    return ex instanceof DataAccessException && ex.getMessage().contains("TooManyRequests");
  }

}
