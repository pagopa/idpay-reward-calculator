package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.reward=WARN",
                "logging.level.it.gov.pagopa.common.web.exception.ErrorManager=OFF",
                "logging.level.it.gov.pagopa.common.reactive.utils.PerformanceLogger=WARN",
                "it.gov.pagopa.reward.service.lookup.ops.DeleteHpanServiceImpl=WARN"
        })
class InstrumentApiControllerIntegrationTest extends BaseApiControllerIntegrationTest {
    @SpyBean
    private HpanInitiativesRepository hpanInitiativesRepositorySpy;

    private final List<FailableConsumer<Integer, Exception>> useCases = new ArrayList<>();

    @Test
    void test() {

        int N = Math.max(useCases.size(), 50);

        configureSpy();

        baseParallelismTest(N, useCases);
    }

    private void configureSpy() {
        Mockito.doThrow(new RuntimeException("DUMMY_EXCEPTION"))
                        .when(hpanInitiativesRepositorySpy).findById((String) Mockito.argThat(arg -> arg.toString().startsWith("EXCEPTIONHPAN")));
    }

    //region useCases
    {
        //usecase 0: delete interval close
        useCases.add(i -> {
            HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(i);
            hpanInitiativesRepositorySpy.save(hpanInitiatives).block();

            extractResponse(cancelInstrument(HpanInitiativeBulkDTOFaker.mockInstanceBuilder(i)
                    .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                    .build()),
                    HttpStatus.NO_CONTENT,
                    null);
            Assertions.assertEquals(hpanInitiatives, hpanInitiativesRepositorySpy.findById(hpanInitiatives.getHpan()).block());
        });

        //usecase 1: delete interval open
        useCases.add(i -> {
            HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(i);
            hpanInitiativesRepositorySpy.save(hpanInitiatives).block();

            HpanInitiativeBulkDTO request = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(i)
                    .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                    .build();

            extractResponse(cancelInstrument(request), HttpStatus.NO_CONTENT, null);

            List<HpanInitiatives> hpanInitiativesFirstCall = retrieveHpanInitiative(request);

            Assertions.assertNotNull(hpanInitiativesFirstCall);
            hpanInitiativesFirstCall.forEach(h -> h.getOnboardedInitiatives()
                    .stream()
                    .filter(onboardedInitiative -> onboardedInitiative.getInitiativeId().equals("INITIATIVE_%d".formatted(i)))
                    .forEach(onboardedInitiative ->
                        onboardedInitiative.getActiveTimeIntervals().forEach(intervals -> Assertions.assertNotNull(intervals.getEndInterval()))));

            /*resend the request*/
            extractResponse(cancelInstrument(request), HttpStatus.NO_CONTENT, null);
            List<HpanInitiatives> hpanInitiativesSecondCall = retrieveHpanInitiative(request);
            Assertions.assertEquals(hpanInitiativesFirstCall, hpanInitiativesSecondCall);
        });

        //usecase 4: Unexpected Operation
        useCases.add(i -> {
            HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(i);
            hpanInitiativesRepositorySpy.save(hpanInitiatives).block();

            extractResponse(cancelInstrument(HpanInitiativeBulkDTOFaker.mockInstanceBuilder(i)
                    .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT).build()),
                    HttpStatus.BAD_REQUEST,
                    null);
        });

        //usecase 5: Generic error
        useCases.add(i -> {
            PaymentMethodInfoDTO infoHpan = PaymentMethodInfoDTO.builder()
                    .hpan("EXCEPTIONHPAN_%d".formatted(i)).build();

            ErrorDTO errorDTOResult = extractResponse(cancelInstrument(HpanInitiativeBulkDTOFaker.mockInstanceBuilder(i)
                            .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                            .infoList(List.of(infoHpan)).build()),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorDTO.class);

            ErrorDTO errorDTOExpected = new ErrorDTO("Error", "Something gone wrong");
            Assertions.assertEquals(errorDTOExpected, errorDTOResult);
        });
    }

    @Nullable
    private List<HpanInitiatives> retrieveHpanInitiative(HpanInitiativeBulkDTO request) {
        return Flux.fromIterable(request.getInfoList())
                .flatMap(paymentMethodInfoDTO -> hpanInitiativesRepositorySpy.findById(paymentMethodInfoDTO.getHpan()))
                .collectList().block();
    }
    //endregion

    //region API invokes
    private WebTestClient.ResponseSpec cancelInstrument(HpanInitiativeBulkDTO hpanInitiativeBulkDTO){
        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/reward/instrument/recess").build())
                .body(BodyInserters.fromValue(hpanInitiativeBulkDTO))
                .exchange();
    }
    //endregion
}