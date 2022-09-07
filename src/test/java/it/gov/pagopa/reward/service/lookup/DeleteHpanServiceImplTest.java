package it.gov.pagopa.reward.service.lookup;


import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2InitialEntityMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class DeleteHpanServiceImplTest {

    @Test
    void deleteHpanBeforeLastActiveInterval(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO1.setOperationDate(time.minusYears(4L));
        hpanInitiativeDTO1.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        HpanInitiativeDTO hpanInitiativeDTO2 = new HpanInitiativeDTO();
        hpanInitiativeDTO2.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO2.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO2.setOperationDate(time.minusYears(1L).minusMonths(3L));
        hpanInitiativeDTO2.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        HpanInitiatives result1 = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO1);
        HpanInitiatives result2 = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO2);

        //Then
        Assertions.assertNull(result1);
        Assertions.assertNull(result2);
    }
/* //TODO fix
    @Test
    void deleteHpanAfterLastActiveIntervalNotClose(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);
        ActiveTimeInterval activeTimeIntervalsInitials = hpanInitiatives.getOnboardedInitiatives().get(0).getActiveTimeIntervals()
                .stream().filter(activeTimeInterval -> activeTimeInterval.getEndInterval()==null).toList().get(0);

        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO1.setOperationDate(time);
        hpanInitiativeDTO1.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        HpanInitiatives result = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO1);

        //Then
        Assertions.assertNotNull(result);

        ActiveTimeInterval activeTimeIntervalAfter = result.getOnboardedInitiatives().get(0).getActiveTimeIntervals().stream().filter(o -> o.getStartInterval().equals(activeTimeIntervalsInitials.getStartInterval())).toList().get(0);
        Assertions.assertEquals(time.with(LocalTime.MAX),activeTimeIntervalAfter.getEndInterval());
        Assertions.assertNotEquals(activeTimeIntervalsInitials,activeTimeIntervalAfter);
        Assertions.assertEquals(activeTimeIntervalsInitials.getStartInterval(),activeTimeIntervalAfter.getStartInterval());
        Assertions.assertNotEquals(activeTimeIntervalsInitials.getEndInterval(),activeTimeIntervalAfter.getEndInterval());
    }

    //TODO fix

    @Test
    void deleteHpanAfterLastActiveIntervalClose(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        List<ActiveTimeInterval> activeTimeIntervalsInitials = hpanInitiatives.getOnboardedInitiatives().get(0).getActiveTimeIntervals();
        LocalDateTime lastActive = Collections.max(activeTimeIntervalsInitials.stream().map(ActiveTimeInterval::getStartInterval).toList());
        ActiveTimeInterval lastActiveIntervalInitials = activeTimeIntervalsInitials.stream().filter(activeTimeInterval -> activeTimeInterval.getStartInterval().equals(lastActive)).toList().get(0);


        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO1.setOperationDate(time);
        hpanInitiativeDTO1.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        HpanInitiatives result = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO1);

        //Then
        Assertions.assertNotNull(result);

        ActiveTimeInterval activeTimeIntervalAfter = result.getOnboardedInitiatives().get(0).getActiveTimeIntervals().stream().filter(o -> o.getStartInterval().equals(lastActive)).toList().get(0);
        Assertions.assertEquals(time.with(LocalTime.MAX),activeTimeIntervalAfter.getEndInterval());
        Assertions.assertNotEquals(lastActiveIntervalInitials,activeTimeIntervalAfter);
        Assertions.assertEquals(lastActiveIntervalInitials.getStartInterval(),activeTimeIntervalAfter.getStartInterval());
        Assertions.assertNotEquals(lastActiveIntervalInitials.getEndInterval(),activeTimeIntervalAfter.getEndInterval());
    }
*/
    @Test
    void deleteHpanPresentWithNotInitiativeId(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("ANOTHER_INITIATIVEID");
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time);
        hpanInitiativeDTO.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        HpanInitiatives result = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO);

        //Then
        Assertions.assertNull(result);
    }

    @Test
    void deleteHpanIntoLastActiveIntervalClose(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now().minusMonths(6L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time);
        hpanInitiativeDTO.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        // When
        HpanInitiatives result = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void hpanNull(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();
        HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper = new HpanInitiativeDTO2InitialEntityMapper();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder().userId("USERID").build();

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setInitiativeId("ANOTHER_INITIATIVE");
        hpanInitiativeDTO.setUserId("USERID");
        hpanInitiativeDTO.setOperationDate(LocalDateTime.now());
        hpanInitiativeDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        HpanInitiatives result = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO);

        // Then
        Assertions.assertNull(result);
    }
}