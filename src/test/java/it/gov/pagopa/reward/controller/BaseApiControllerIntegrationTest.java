//package it.gov.pagopa.reward.controller; todo remove me
//
//import it.gov.pagopa.common.web.exception.ErrorManager;
//import it.gov.pagopa.reward.BaseIntegrationTest;
//import org.apache.commons.lang3.function.FailableConsumer;
//import org.junit.jupiter.api.Assertions;
//import org.mockito.Mockito;
//import org.opentest4j.AssertionFailedError;
//import org.springframework.boot.test.mock.mockito.SpyBean;
//import org.springframework.http.HttpStatus;
//import org.springframework.test.web.reactive.server.WebTestClient;
//
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.stream.IntStream;
//
//public class BaseApiControllerIntegrationTest extends BaseIntegrationTest {
//    private static final int parallelism = 8;
//    private static final ExecutorService executor = Executors.newFixedThreadPool(parallelism);
//    @SpyBean
//    protected ErrorManager errorManagerSpy;
//
//    protected void baseParallelismTest(int N, List<FailableConsumer<Integer, Exception>> useCases ) {
//        List<? extends Future<?>> tasks = IntStream.range(0, N)
//                .mapToObj(i -> executor.submit(() -> {
//                    try {
//                        useCases.get(i % useCases.size()).accept(i);
//                    } catch (Exception e) {
//                        throw new IllegalStateException("Unexpected exception thrown during test", e);
//                    }
//                }))
//                .toList();
//
//        for (int i = 0; i < tasks.size(); i++) {
//            try {
//                tasks.get(i).get();
//            } catch (Exception e) {
//                System.err.printf("UseCase %d (bias %d) failed %n", i % useCases.size(), i);
//                Mockito.mockingDetails(errorManagerSpy).getInvocations()
//                        .stream()
//                        .filter(ex->!ex.getArgument(0).getClass().equals(RuntimeException.class))
//                        .forEach(ex -> System.err.println("ErrorManager invocation: " + ex));
//                if (e instanceof RuntimeException runtimeException) {
//                    throw runtimeException;
//                } else if (e.getCause() instanceof AssertionFailedError assertionFailedError) {
//                    throw assertionFailedError;
//                }
//                Assertions.fail(e);
//            }
//        }
//    }
//
//    protected <T> T extractResponse(WebTestClient.ResponseSpec response, HttpStatus expectedHttpStatus, Class<T> expectedBodyClass) {
//        response = response.expectStatus().value(httpStatus -> Assertions.assertEquals(expectedHttpStatus.value(), httpStatus));
//        if (expectedBodyClass != null) {
//            return response.expectBody(expectedBodyClass).returnResult().getResponseBody();
//        }
//        return null;
//    }
//}
