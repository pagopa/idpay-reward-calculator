package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    private final ErrorDTO templateValidationErrorDTO;

    public ValidationExceptionHandler(@Nullable ErrorDTO templateValidationErrorDTO) {
        this.templateValidationErrorDTO = Optional.ofNullable(templateValidationErrorDTO)
                .orElse(new ErrorDTO("INVALID_REQUEST", "Invalid request"));
    }

  @ExceptionHandler(WebExchangeBindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorDTO handleWebExchangeBindException(
          WebExchangeBindException ex, ServerWebExchange exchange) {

    String message = ex.getBindingResult().getAllErrors().stream()
        .map(error -> {
          String fieldName = ((FieldError) error).getField();
          String errorMessage = error.getDefaultMessage();
          return String.format("[%s]: %s", fieldName, errorMessage);
        }).collect(Collectors.joining("; "));

    log.info("A MethodArgumentNotValidException occurred handling request {}: HttpStatus 400 - {}",
        ErrorManager.getRequestDetails(exchange), message);
    log.debug("Something went wrong while validating http request", ex);
    return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
  }

  @ExceptionHandler(MissingRequestValueException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorDTO handleMissingRequestValueException(
          MissingRequestValueException ex, ServerWebExchange exchange) {

    String message = ex.getReason();

    log.info("A MissingRequestValueException occurred handling request {}: HttpStatus 400 - {}",
        ErrorManager.getRequestDetails(exchange), message);
    log.debug("Something went wrong handling request", ex);
    return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
  }
}
