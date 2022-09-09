package it.gov.pagopa.reward.service.lookup;


import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class DeleteHpanServiceImplTest {

    @Test
    void deleteHpanBeforeLastActiveInterval() {
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

    @Test
    void deleteHpanAfterLastActiveIntervalNotClose() {
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);

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

        List<ActiveTimeInterval> activeTimeIntervalsResult = result.getOnboardedInitiatives().get(0).getActiveTimeIntervals();
        Assertions.assertEquals(2, activeTimeIntervalsResult.size());

        ActiveTimeInterval activeIntervalExpected = ActiveTimeInterval.builder()
                .startInterval(LocalDateTime.now().plusDays(5L).with(LocalTime.MIN).plusDays(1L))
                .endInterval(time.with(LocalTime.MAX)).build();
        Assertions.assertTrue(activeTimeIntervalsResult.contains(activeIntervalExpected));

        ActiveTimeInterval activeIntervalAfter = activeTimeIntervalsResult.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval)).get();
        Assertions.assertEquals(activeIntervalExpected.getStartInterval(),activeIntervalAfter.getStartInterval());
        Assertions.assertNotNull(activeIntervalAfter.getEndInterval());
        Assertions.assertEquals(activeIntervalExpected.getEndInterval(), activeIntervalAfter.getEndInterval());
    }

    @Test
    void deleteHpanAfterLastActiveIntervalClose() {
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime time = now.plusMonths(2L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

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
        List<ActiveTimeInterval> activeTimeIntervalsResult = result.getOnboardedInitiatives().get(0).getActiveTimeIntervals();
        Assertions.assertEquals(2, activeTimeIntervalsResult.size());

        ActiveTimeInterval activeTimeIntervalAfter = activeTimeIntervalsResult.stream().filter(o -> o.getStartInterval().equals(now.minusYears(1L).with(LocalTime.MIN).plusDays(1L))).toList().get(0);
        Assertions.assertEquals(time.with(LocalTime.MAX), activeTimeIntervalAfter.getEndInterval());

        ActiveTimeInterval intervalBefore= ActiveTimeInterval.builder().startInterval(now.minusYears(1L).with(LocalTime.MIN).plusDays(1L))
                .endInterval(now.minusMonths(5L).with(LocalTime.MAX)).build();

        Assertions.assertNotEquals(intervalBefore, activeTimeIntervalAfter);
        Assertions.assertEquals(intervalBefore.getStartInterval(), activeTimeIntervalAfter.getStartInterval());
        Assertions.assertNotEquals(intervalBefore.getEndInterval(), activeTimeIntervalAfter.getEndInterval());
    }

    @Test
    void deleteHpanPresentWithNotInitiativeId() {
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
    void deleteHpanIntoLastActiveIntervalClose() {
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
    void hpanNull() {
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();
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

    @Test
    void unexpectedHpanWithoutInitiatives(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID").hpan("HPAN").build();

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVEID");
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(LocalDateTime.now());
        hpanInitiativeDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        HpanInitiatives result = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void unexpectedInitiativeWithoutIntervals(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID").status("ACTIVE").build();
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVEID");
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(LocalDateTime.now());
        hpanInitiativeDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        HpanInitiatives result = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void unexpectedNotMaxActiveInterval(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID")
                .status("ACTIVE")
                .activeTimeIntervals(new ArrayList<>()).build();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVEID");
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(LocalDateTime.now());
        hpanInitiativeDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        HpanInitiatives result = deleteHpanService.execute(hpanInitiatives, hpanInitiativeDTO);

        // Then
        Assertions.assertNull(result);
    }
}