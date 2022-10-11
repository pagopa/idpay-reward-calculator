package it.gov.pagopa.reward.service.lookup.ops;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
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

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO1.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO1.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO1.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO1.setEvaluationDate(time.minusYears(4L));
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO2 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO2.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO2.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO2.setEvaluationDate(time.minusYears(1L).minusMonths(3L));
        hpanUpdateEvaluateDTO2.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        OnboardedInitiative result1 = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO1);
        OnboardedInitiative result2 = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO2);

        //Then
        Assertions.assertNull(result1);
        Assertions.assertNull(result2);
    }

    @Test
    void deleteHpanAfterLastActiveIntervalNotClose() {
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO1.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO1.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO1.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO1.setEvaluationDate(time);
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO1);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result);

        List<ActiveTimeInterval> activeTimeIntervalsResult = result.getActiveTimeIntervals();
        Assertions.assertEquals(2, activeTimeIntervalsResult.size());

        ActiveTimeInterval activeIntervalExpected = ActiveTimeInterval.builder()
                .startInterval(time.minusMonths(5L).with(LocalTime.MIN).plusDays(1L))
                .endInterval(time.with(LocalTime.MAX)).build();

        ActiveTimeInterval activeIntervalAfter = activeTimeIntervalsResult.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval)).orElse(null);
        Assertions.assertNotNull(activeIntervalAfter);
        Assertions.assertEquals(activeIntervalExpected.getStartInterval(),activeIntervalAfter.getStartInterval());
        Assertions.assertNotNull(activeIntervalAfter.getEndInterval());
        Assertions.assertEquals(activeIntervalExpected.getEndInterval(), activeIntervalAfter.getEndInterval());
        Assertions.assertNotNull(result.getLastEndInterval());
        Assertions.assertEquals(result.getLastEndInterval(), activeIntervalExpected.getEndInterval());
    }

    @Test
    void deleteHpanAfterLastActiveIntervalClose() {
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO1.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO1.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO1.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO1.setEvaluationDate(time);
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO1);

        //Then
        Assertions.assertNull(result);
    }

    @Test
    void deleteHpanPresentWithNotInitiativeId() {
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setInitiativeId("ANOTHER_INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

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

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void hpanNull() {
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder().userId("USERID").build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setInitiativeId("ANOTHER_INITIATIVE");
        hpanUpdateEvaluateDTO.setUserId("USERID");
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now());
        hpanUpdateEvaluateDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void unexpectedHpanWithoutInitiatives(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID").hpan("HPAN").build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now());
        hpanUpdateEvaluateDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

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

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now());
        hpanUpdateEvaluateDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void unexpectedEmptyActiveInterval(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID").status("ACTIVE").activeTimeIntervals(new ArrayList<>()).build();
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now());
        hpanUpdateEvaluateDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

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

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now());
        hpanUpdateEvaluateDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }
}