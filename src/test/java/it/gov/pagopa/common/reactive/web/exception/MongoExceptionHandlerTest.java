package it.gov.pagopa.common.reactive.web.exception;

import com.mongodb.MongoQueryException;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteError;
import it.gov.pagopa.common.reactive.mongo.retry.exception.MongoRequestRateTooLargeRetryExpiredException;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.common.web.exception.ErrorManager;
import it.gov.pagopa.common.web.exception.MongoExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode;
import static org.mockito.Mockito.doThrow;
@ExtendWith(SpringExtension.class)
@WebFluxTest
@ContextConfiguration(classes = {MongoExceptionHandler.class,
    MongoExceptionHandlerTest.TestController.class, ErrorManager.class})
class MongoExceptionHandlerTest {

  public static final ErrorDTO EXPECTED_DEFAULT_ERROR = new ErrorDTO("Error", "Something gone wrong");
  public static final ErrorDTO EXPECTED_TOO_MANY_REQUESTS_ERROR = new ErrorDTO("TOO_MANY_REQUESTS", "Too Many Requests");

  @Autowired
  private WebTestClient webTestClient;

  @SpyBean
  private TestController testControllerSpy;


  @RestController
  @Slf4j
  static class TestController {

    @GetMapping("/test")
    String testEndpoint() {
      return "OK";
    }
  }


  @Test
  void handleUncategorizedMongoDbException() {

    String mongoFullErrorResponse = """
        {"ok": 0.0, "errmsg": "Error=16500, RetryAfterMs=34,\s
        Details='Response status code does not indicate success: TooManyRequests (429) Substatus: 3200 ActivityId: 46ba3855-bc3b-4670-8609-17e1c2c87778 Reason:\s
        (\\r\\nErrors : [\\r\\n \\"Request rate is large. More Request Units may be needed, so no changes were made. Please retry this request later. Learn more:
         http://aka.ms/cosmosdb-error-429\\"\\r\\n]\\r\\n) ", "code": 16500, "codeName": "RequestRateTooLarge"}
        """;

    final MongoQueryException mongoQueryException = new MongoQueryException(
        BsonDocument.parse(mongoFullErrorResponse), new ServerAddress());
    doThrow(
        new UncategorizedMongoDbException(mongoQueryException.getMessage(), mongoQueryException))
        .when(testControllerSpy).testEndpoint();
    ErrorDTO expectedErrorDefault = new ErrorDTO(ExceptionCode.TOO_MANY_REQUESTS,"TOO_MANY_REQUESTS");

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("/test").build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        .expectHeader().exists(HttpHeaders.RETRY_AFTER)
        .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "1")
        .expectHeader().valueEquals("Retry-After-Ms", "34")
        .expectBody(ErrorDTO.class).isEqualTo(EXPECTED_TOO_MANY_REQUESTS_ERROR);
  }

  @Test
  void handleTooManyWriteDbException() {

    String writeErrorMessage = """
            Error=16500, RetryAfterMs=34, Details='Response status code does not indicate success: TooManyRequests (429); Substatus: 3200; ActivityId: 822d212d-5aac-4f5d-a2d4-76d6da7b619e; Reason: (
            Errors : [
              "Request rate is large. More Request Units may be needed, so no changes were made. Please retry this request later. Learn more: http://aka.ms/cosmosdb-error-429"
            ]
            );
            """;

    final MongoWriteException mongoWriteException = new MongoWriteException(
            new WriteError(16500, writeErrorMessage, BsonDocument.parse("{}")), new ServerAddress());
    doThrow(
            new DataIntegrityViolationException(mongoWriteException.getMessage(), mongoWriteException))
            .when(testControllerSpy).testEndpoint();
    ErrorDTO expectedErrorDefault = new ErrorDTO(ExceptionCode.TOO_MANY_REQUESTS,"TOO_MANY_REQUESTS");

    webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/test").build())
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectHeader().exists(HttpHeaders.RETRY_AFTER)
            .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "1")
            .expectHeader().valueEquals("Retry-After-Ms", "34")
            .expectBody(ErrorDTO.class).isEqualTo(EXPECTED_TOO_MANY_REQUESTS_ERROR);
  }

  @Test
  void handleUncategorizedMongoDbExceptionNotRequestRateTooLarge() {

    doThrow(new UncategorizedMongoDbException("DUMMY", new Exception()))
        .when(testControllerSpy).testEndpoint();

    ErrorDTO expectedErrorDefault = new ErrorDTO(ExceptionCode.GENERIC_ERROR,"Something gone wrong");

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("/test").build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        .expectBody(ErrorDTO.class).isEqualTo(EXPECTED_DEFAULT_ERROR);
  }

  @Test
  void handleMongoRequestRateTooLargeRetryExpiredException() {
    doThrow(new MongoRequestRateTooLargeRetryExpiredException("FLOWNAME",3,3,0,100,34L,new Exception()))
        .when(testControllerSpy).testEndpoint();

    ErrorDTO expectedErrorDefault = new ErrorDTO(ExceptionCode.TOO_MANY_REQUESTS,"TOO_MANY_REQUESTS");

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("/test").build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        .expectBody(ErrorDTO.class).isEqualTo(EXPECTED_TOO_MANY_REQUESTS_ERROR);
  }
}
