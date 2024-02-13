package it.gov.pagopa.common.reactive.web.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.web.exception.ErrorManager;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@WebFluxTest(value = {ValidationExceptionHandlerTest.TestController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
        ValidationExceptionHandlerTest.TestController.class,
        ValidationExceptionHandler.class,
        ErrorManager.class})
class ValidationExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private TestController testControllerSpy;

    @RestController
    @Slf4j
    static class TestController {

        @PutMapping("/test")
        String testEndpoint(@RequestBody @Valid ValidationDTO body, @RequestHeader("data") String data) {
            return "OK";
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ValidationDTO {
        @NotBlank(message = "The field is mandatory!")
        private String data;
    }

    private final ValidationDTO VALIDATION_DTO = new ValidationDTO("data");

    @Test
    void handleMethodArgumentNotValidException() throws Exception {
        webTestClient.put()
                .uri("/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(new ValidationDTO("")))
                .header("data", "data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .json("{\"code\":\"INVALID_REQUEST\",\"message\":\"[data]: The field is mandatory!\"}");
    }

    @Test
    void handleMissingRequestHeaderException() throws Exception {
        webTestClient.put()
                .uri("/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(VALIDATION_DTO))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .json("{\"code\":\"INVALID_REQUEST\",\"message\":\"Required header 'data' is not present.\"}");
    }
}