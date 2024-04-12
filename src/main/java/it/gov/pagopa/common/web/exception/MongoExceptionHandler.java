package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.reactive.mongo.retry.MongoRequestRateTooLargeRetryer;
import it.gov.pagopa.common.reactive.mongo.retry.exception.MongoRequestRateTooLargeRetryExpiredException;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MongoExceptionHandler {

  private final ErrorManager errorManager;

  private final ErrorDTO tooManyRequestsErrorDTO;

  public MongoExceptionHandler(ErrorManager errorManager, @Nullable ErrorDTO tooManyRequestsErrorDTO) {
    this.errorManager = errorManager;

    this.tooManyRequestsErrorDTO = Optional.ofNullable(tooManyRequestsErrorDTO)
            .orElse(new ErrorDTO("TOO_MANY_REQUESTS", "Too Many Requests"));
  }


  @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<ErrorDTO> handleDataAccessException(
          DataAccessException ex, ServerWebExchange request) {

    if (MongoRequestRateTooLargeRetryer.isRequestRateTooLargeException(ex)) {
      Long retryAfterMs = MongoRequestRateTooLargeRetryer.getRetryAfterMs(ex);

      return getErrorDTOResponseEntity(ex, request, retryAfterMs);
    } else {
      return errorManager.handleException(ex, request);
    }
  }

  @ExceptionHandler(MongoRequestRateTooLargeRetryExpiredException.class)
  protected ResponseEntity<ErrorDTO> handleMongoRequestRateTooLargeRetryExpiredException(
      MongoRequestRateTooLargeRetryExpiredException ex, ServerWebExchange request) {

    return getErrorDTOResponseEntity(ex, request, ex.getRetryAfterMs());
  }

  private ResponseEntity<ErrorDTO> getErrorDTOResponseEntity(Exception ex,
      ServerWebExchange request, Long retryAfterMs) {
    String message = ex.getMessage();

    log.info(
        "A MongoQueryException (RequestRateTooLarge) occurred handling request {}: HttpStatus 429 - {}",
        ErrorManager.getRequestDetails(request), message);
    log.debug("Something went wrong while accessing MongoDB", ex);

    final BodyBuilder bodyBuilder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .contentType(MediaType.APPLICATION_JSON);

    if (retryAfterMs != null) {
      long retryAfter = (long) Math.ceil((double) retryAfterMs / 1000);
      bodyBuilder.header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter))
          .header("Retry-After-Ms", String.valueOf(retryAfterMs));
    }

    return bodyBuilder
        .body(tooManyRequestsErrorDTO);
  }

}
