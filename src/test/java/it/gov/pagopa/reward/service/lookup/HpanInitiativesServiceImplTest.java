package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2InitialEntityMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

class HpanInitiativesServiceImplTest {
    @Test
    void addInstrument(){
        // Given
        AddHpanService addHpanService = Mockito.mock(AddHpanServiceImpl.class);
        DeleteHpanService deleteHpanService = Mockito.mock(DeleteHpanServiceImpl.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(addHpanService, deleteHpanService);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(1);
        HpanInitiativeDTO hpanInitiativeDTO = HpanInitiativeDTOFaker.mockInstanceBuilder(1)
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now())
                .build();

        Mockito.when(addHpanService.execute(hpanInitiatives, hpanInitiativeDTO)).thenReturn(hpanInitiatives);

        // When
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(hpanInitiatives, result);
        Mockito.verify(addHpanService, Mockito.only()).execute(Mockito.same(hpanInitiatives), Mockito.same(hpanInitiativeDTO));
        Mockito.verify(deleteHpanService, Mockito.never()).execute(Mockito.any(HpanInitiatives.class), Mockito.any(HpanInitiativeDTO.class));
    }


    @Test
    void deleteInstrument(){
        // Given
        AddHpanService addHpanService = Mockito.mock(AddHpanServiceImpl.class);
        DeleteHpanService deleteHpanService = Mockito.mock(DeleteHpanServiceImpl.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(addHpanService, deleteHpanService);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(1);
        HpanInitiativeDTO hpanInitiativeDTO = HpanInitiativeDTOFaker.mockInstanceBuilder(1)
                .operationType(HpanInitiativeConstants.DELETE_INSTRUMENT)
                .operationDate(LocalDateTime.now())
                .build();

        Mockito.when(deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO)).thenReturn(hpanInitiatives);

        // When
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(hpanInitiatives, result);
        Mockito.verify(deleteHpanService, Mockito.only()).execute(Mockito.same(hpanInitiatives), Mockito.same(hpanInitiativeDTO));
        Mockito.verify(addHpanService, Mockito.never()).execute(Mockito.any(HpanInitiatives.class), Mockito.any(HpanInitiativeDTO.class));
    }

    @Test
    void invalidOperation(){
        // Given
        AddHpanService addHpanService = Mockito.mock(AddHpanServiceImpl.class);
        DeleteHpanService deleteHpanService = Mockito.mock(DeleteHpanServiceImpl.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(addHpanService, deleteHpanService);
        int bias=1;
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .hpan("HPAN_%d".formatted(bias))
                .userId("USERID_%d".formatted(bias)).build();

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("ANOTHER_INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(LocalDateTime.now());
        hpanInitiativeDTO.setOperationType("COMMAND_INSTRUMENT");

        //When
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

        // Then
        Assertions.assertNull(result);
    }

}