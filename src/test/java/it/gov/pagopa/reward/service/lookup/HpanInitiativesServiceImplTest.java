package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.service.lookup.ops.AddHpanService;
import it.gov.pagopa.reward.service.lookup.ops.AddHpanServiceImpl;
import it.gov.pagopa.reward.service.lookup.ops.DeleteHpanService;
import it.gov.pagopa.reward.service.lookup.ops.DeleteHpanServiceImpl;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
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
        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = HpanInitiativeDTOFaker.mockInstanceBuilder(1)
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .evaluationDate(LocalDateTime.now())
                .build();

        OnboardedInitiative onboardedInitiativeOut = OnboardedInitiative.builder()
                .initiativeId("INITIATIVEID_OUT")
                .status("ACEPTED").build();

        Mockito.when(addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO)).thenReturn(onboardedInitiativeOut);

        // When
        OnboardedInitiative result = hpanInitiativesService.evaluate(hpanUpdateEvaluateDTO, hpanInitiatives);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(onboardedInitiativeOut, result);
        Mockito.verify(addHpanService, Mockito.only()).execute(Mockito.same(hpanInitiatives), Mockito.same(hpanUpdateEvaluateDTO));
        Mockito.verify(deleteHpanService, Mockito.never()).execute(Mockito.any(HpanInitiatives.class), Mockito.any(HpanUpdateEvaluateDTO.class));
    }

    @Test
    void deleteInstrument(){
        // Given
        AddHpanService addHpanService = Mockito.mock(AddHpanServiceImpl.class);
        DeleteHpanService deleteHpanService = Mockito.mock(DeleteHpanServiceImpl.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(addHpanService, deleteHpanService);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(1);
        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = HpanInitiativeDTOFaker.mockInstanceBuilder(1)
                .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                .evaluationDate(LocalDateTime.now())
                .build();

        OnboardedInitiative onboardedInitiativeOut = OnboardedInitiative.builder()
                .initiativeId("INITIATIVEID_OUT")
                .status("ACEPTED").build();

        Mockito.when(deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO)).thenReturn(onboardedInitiativeOut);

        // When
        OnboardedInitiative result = hpanInitiativesService.evaluate(hpanUpdateEvaluateDTO, hpanInitiatives);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(onboardedInitiativeOut, result);
        Mockito.verify(deleteHpanService, Mockito.only()).execute(Mockito.same(hpanInitiatives), Mockito.same(hpanUpdateEvaluateDTO));
        Mockito.verify(addHpanService, Mockito.never()).execute(Mockito.any(HpanInitiatives.class), Mockito.any(HpanUpdateEvaluateDTO.class));
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

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setInitiativeId("ANOTHER_INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now());
        hpanUpdateEvaluateDTO.setOperationType("COMMAND_INSTRUMENT");

        //When
        OnboardedInitiative result = hpanInitiativesService.evaluate(hpanUpdateEvaluateDTO, hpanInitiatives);

        // Then
        Assertions.assertNull(result);
        Mockito.verify(addHpanService, Mockito.never()).execute(Mockito.any(HpanInitiatives.class), Mockito.any(HpanUpdateEvaluateDTO.class));
        Mockito.verify(deleteHpanService, Mockito.never()).execute(Mockito.any(HpanInitiatives.class), Mockito.any(HpanUpdateEvaluateDTO.class));
    }
}