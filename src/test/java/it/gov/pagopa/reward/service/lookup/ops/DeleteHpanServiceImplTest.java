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

    private final DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

    @Test
    void deleteHpanBeforeLastActiveInterval() {
        // Given
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
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
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO1.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO1.setEvaluationDate(time);
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        //When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO1);

        //Then
        Assertions.assertNotNull(result);

        List<ActiveTimeInterval> activeTimeIntervalsResult = result.getActiveTimeIntervals();
        Assertions.assertEquals(2, activeTimeIntervalsResult.size());

        ActiveTimeInterval activeIntervalExpected = ActiveTimeInterval.builder()
                .startInterval(time.minusMonths(5L).with(LocalTime.MIN).plusDays(1L))
                .endInterval(time.plusDays(1).with(LocalTime.MIN)).build();

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
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
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
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
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
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().minusMonths(6L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
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
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID").hpan("HPAN").build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
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
        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID").status("ACTIVE").build();
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
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
        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID").status("ACTIVE").activeTimeIntervals(new ArrayList<>()).build();
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
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
        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID")
                .status("ACTIVE")
                .activeTimeIntervals(new ArrayList<>()).build();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
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
    void testActivationDeletionSameDay() {
        // Given
        int bias = 1;
        String initiativeId = "INITIATIVE_%d".formatted(bias);

        LocalDateTime elabDateTime = LocalDateTime.now();
        LocalDateTime expectedDateTime = elabDateTime.plusDays(1).with(LocalTime.MIN);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        hpanInitiatives.getOnboardedInitiatives().get(0)
                .getActiveTimeIntervals()
                .add(new ActiveTimeInterval(expectedDateTime, null));

        HpanUpdateEvaluateDTO request = new HpanUpdateEvaluateDTO();
        request.setHpan(hpanInitiatives.getHpan());
        request.setMaskedPan(hpanInitiatives.getMaskedPan());
        request.setBrandLogo(hpanInitiatives.getBrandLogo());
        request.setInitiativeId(initiativeId);
        request.setUserId(hpanInitiatives.getUserId());
        request.setEvaluationDate(elabDateTime);
        request.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        // When
        OnboardedInitiative result1 = deleteHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(2, result1.getActiveTimeIntervals().size());
        ActiveTimeInterval lastInterval= result1.getActiveTimeIntervals().get(result1.getActiveTimeIntervals().size() - 1);
        Assertions.assertNotNull(lastInterval.getEndInterval());
        Assertions.assertEquals(lastInterval.getEndInterval(), result1.getLastEndInterval());

        // When adding again
        AddHpanService addHpanService = new AddHpanServiceImpl();
        OnboardedInitiative result2 = addHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(3, result2.getActiveTimeIntervals().size());
        ActiveTimeInterval lastInterval2 = result2.getActiveTimeIntervals().get(result2.getActiveTimeIntervals().size() - 1);
        Assertions.assertEquals(expectedDateTime, lastInterval2.getStartInterval());
        Assertions.assertNull(lastInterval2.getEndInterval());
        Assertions.assertNull(result2.getLastEndInterval());

        // When deleting again
        OnboardedInitiative result3 = deleteHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(2, result3.getActiveTimeIntervals().size());
        ActiveTimeInterval lastInterval3= result3.getActiveTimeIntervals().get(result3.getActiveTimeIntervals().size() - 1);
        Assertions.assertNotNull(lastInterval3.getEndInterval());
        Assertions.assertEquals(lastInterval3.getEndInterval(), result3.getLastEndInterval());
    }

    @Test
    void testActivationDeletionSameDayNoInitialInitiatives() {
        // Given
        int bias = 1;
        String initiativeId = "INITIATIVE_%d".formatted(bias);

        LocalDateTime elabDateTime = LocalDateTime.now();
        LocalDateTime expectedDateTime = elabDateTime.plusDays(1).with(LocalTime.MIN);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(bias);

        HpanUpdateEvaluateDTO request = new HpanUpdateEvaluateDTO();
        request.setHpan(hpanInitiatives.getHpan());
        request.setMaskedPan(hpanInitiatives.getMaskedPan());
        request.setBrandLogo(hpanInitiatives.getBrandLogo());
        request.setInitiativeId(initiativeId);
        request.setUserId(hpanInitiatives.getUserId());
        request.setEvaluationDate(elabDateTime);
        request.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        // When adding first interval
        AddHpanService addHpanService = new AddHpanServiceImpl();
        OnboardedInitiative firstAddResult = addHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(1, firstAddResult.getActiveTimeIntervals().size());
        ActiveTimeInterval interval = firstAddResult.getActiveTimeIntervals().get(0);
        Assertions.assertEquals(expectedDateTime, interval.getStartInterval());
        Assertions.assertNull(interval.getEndInterval());
        hpanInitiatives.setOnboardedInitiatives(new ArrayList<>(List.of(firstAddResult)));

        // When deleting it
        OnboardedInitiative result1 = deleteHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(0, result1.getActiveTimeIntervals().size());
        hpanInitiatives.getOnboardedInitiatives().remove(firstAddResult);

        // When adding again
        OnboardedInitiative result2 = addHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(1, result2.getActiveTimeIntervals().size());
        ActiveTimeInterval lastInterval2 = result2.getActiveTimeIntervals().get(result2.getActiveTimeIntervals().size() - 1);
        Assertions.assertEquals(expectedDateTime, lastInterval2.getStartInterval());
        Assertions.assertNull(lastInterval2.getEndInterval());

        // When deleting again
        OnboardedInitiative result3 = deleteHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(0, result3.getActiveTimeIntervals().size());
    }
}