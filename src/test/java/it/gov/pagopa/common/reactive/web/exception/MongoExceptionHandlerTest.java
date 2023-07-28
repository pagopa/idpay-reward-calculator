package it.gov.pagopa.common.reactive.web.exception;

import static org.mockito.Mockito.doThrow;

import com.mongodb.MongoQueryException;
import com.mongodb.ServerAddress;
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
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(SpringExtension.class)
@WebFluxTest
@ContextConfiguration(classes = {MongoExceptionHandler.class,
    MongoExceptionHandlerTest.TestController.class, ErrorManager.class})
class MongoExceptionHandlerTest {

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
    ErrorDTO expectedErrorDefault = new ErrorDTO("TOO_MANY_REQUESTS","TOO_MANY_REQUESTS");

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("/test").build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        .expectHeader().exists(HttpHeaders.RETRY_AFTER)
        .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "1")
        .expectHeader().valueEquals("Retry-After-Ms", "34")
        .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDefault);
  }

  @Test
  void handleUncategorizedMongoDbExceptionNotRequestRateTooLarge() {

    doThrow(new UncategorizedMongoDbException("TooManyRequests", new Exception()))
        .when(testControllerSpy).testEndpoint();

    ErrorDTO expectedErrorDefault = new ErrorDTO("Error","Something gone wrong");

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("/test").build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDefault);
  }

  @Test
  void handleMongoRequestRateTooLargeRetryExpiredException() {
    doThrow(new MongoRequestRateTooLargeRetryExpiredException(3,3,0,100,34L,new Exception()))
        .when(testControllerSpy).testEndpoint();

    ErrorDTO expectedErrorDefault = new ErrorDTO("TOO_MANY_REQUESTS","TOO_MANY_REQUESTS");

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("/test").build())
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        .expectBody(ErrorDTO.class).isEqualTo(expectedErrorDefault);
  }
}
