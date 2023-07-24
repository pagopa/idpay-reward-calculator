package it.gov.pagopa.common.mongo.retry.exception;

import lombok.Getter;

@Getter
public class MongoRequestRateTooLargeRetryExpiredException extends RuntimeException {

  private final long maxRetry;
  private final long counter;
  private final long maxMillisElapsed;
  private final long millisElapsed;
  private final Long retryAfterMs;


  public MongoRequestRateTooLargeRetryExpiredException(long maxRetry, long counter,
      long maxMillisElapsed, long millisElapsed, Long retryAfterMs, Throwable cause) {
    super("[REQUEST_RATE_TOO_LARGE_RETRY_EXPIRED] Expired retry for RequestRateTooLargeException: attempt %d of %d after %d ms of max %d ms, suggested retry after %s ms"
        .formatted(counter, maxRetry, millisElapsed, maxMillisElapsed, String.valueOf(retryAfterMs)),
        cause);
    this.maxRetry = maxRetry;
    this.counter = counter;
    this.maxMillisElapsed = maxMillisElapsed;
    this.millisElapsed = millisElapsed;
    this.retryAfterMs = retryAfterMs;
  }

}
