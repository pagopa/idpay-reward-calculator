package it.gov.pagopa.reward.service.recess;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import it.gov.pagopa.reward.service.lookup.HpanInitiativeMediatorService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class InstrumentApiServiceImplTest {

    @Mock HpanInitiativeMediatorService hpanInitiativeMediatorServiceMock;

    InstrumentApiServiceImpl instrumentApiService;

    @BeforeEach
    void setUp() {
        instrumentApiService= new InstrumentApiServiceImpl(hpanInitiativeMediatorServiceMock);
    }

    @Test
    void cancelInstrument() {
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOBuilder = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(1).operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT).build();
        List<String> hpanList = hpanInitiativeBulkDTOBuilder.getInfoList().stream().map(PaymentMethodInfoDTO::getHpan).toList();
        Mockito.doReturn(Flux.fromIterable(hpanList))
                .when(hpanInitiativeMediatorServiceMock).evaluate(Mockito.eq(hpanInitiativeBulkDTOBuilder), Mockito.any());

        List<String> result = instrumentApiService.cancelInstruments(hpanInitiativeBulkDTOBuilder).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(hpanList, result);
    }

    @Test
    void cancelInstrumentNotValidOperation() {
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOBuilder = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(1).operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT).build();

        Mono<List<String>> result = instrumentApiService.cancelInstruments(hpanInitiativeBulkDTOBuilder);

        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(IllegalArgumentException.class, result::block);
        Assertions.assertEquals("[SYNC_CANCEL_INSTRUMENTS] Operation type not valid", illegalArgumentException.getMessage());
    }
}