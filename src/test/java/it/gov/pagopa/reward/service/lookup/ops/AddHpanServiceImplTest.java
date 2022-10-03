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
import java.util.List;

class AddHpanServiceImplTest {

    @Test
    void addHpanAfterLastIntervalClose() {
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        int activeIntervalsInitial = hpanInitiatives.getOnboardedInitiatives().size();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
         OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNotNull(result);

        List<ActiveTimeInterval> activeTimeIntervals = result.getActiveTimeIntervals();
        Assertions.assertEquals(3,activeTimeIntervals.size());
        Assertions.assertTrue(activeTimeIntervals.contains(ActiveTimeInterval.builder().startInterval(time.with(LocalTime.MIN).plusDays(1L)).build()));
        Assertions.assertNotEquals(activeIntervalsInitial,activeTimeIntervals.size());

        Assertions.assertNull(result.getLastEndInterval());
    }

    @Test
    void addHpanAfterLasIntervalOpen(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);

        String initiativeId = "INITIATIVE_%d".formatted(bias);
        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setInitiativeId(initiativeId);
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        //When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);
        //Then
        Assertions.assertNotNull(result);


        List<ActiveTimeInterval> activeTimeIntervalsResult = result.getActiveTimeIntervals();
        Assertions.assertEquals(3, activeTimeIntervalsResult.size());

        ActiveTimeInterval intervalChangeExpected = ActiveTimeInterval.builder()
                .startInterval(time.minusMonths(5L).with(LocalTime.MIN).plusDays(1L))
                .endInterval(time.with(LocalTime.MAX)).build();
        Assertions.assertTrue(activeTimeIntervalsResult.contains(intervalChangeExpected));

        ActiveTimeInterval newIntervalExpected = ActiveTimeInterval.builder()
                .startInterval(time.with(LocalTime.MIN).plusDays(1)).build();
        Assertions.assertTrue(activeTimeIntervalsResult.contains(newIntervalExpected));
        Assertions.assertNull(result.getLastEndInterval());
    }

    @Test
    void addHpanIntoLastIntervalClose() {
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().minusMonths(6L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);
        // Then
        Assertions.assertNull(result);
    }

    @Test
    void addHpanBeforeLastActiveInterval(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO1.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO1.setEvaluationDate(time.minusYears(4L));
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO2 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO2.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO2.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO2.setEvaluationDate(time.minusYears(1L).minusMonths(3L));
        hpanUpdateEvaluateDTO2.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        OnboardedInitiative resultBeforeAllActiveIntervals = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO1);
        OnboardedInitiative resultBeforeLastActiveIntervalsAndAfterAnyInterval = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO2);

        // Then
        Assertions.assertNull(resultBeforeAllActiveIntervals);
        Assertions.assertNull(resultBeforeLastActiveIntervalsAndAfterAnyInterval);
    }

    @Test
    void addHpanWithNewInitiative(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        String newInitiativeId = String.format("ANOTHER_INITIATIVE_%d",bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setInitiativeId(newInitiativeId);
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getLastEndInterval());
    }

    @Test
    void addHpanWithNoneInitiative(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .hpan("HPAN_%d".formatted(bias))
                .userId("USERID_%d".formatted(bias)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setInitiativeId("First_INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getLastEndInterval());
    }

    @Test
    void unexpectedNotMaxActiveInterval(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();

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
        hpanUpdateEvaluateDTO.setOperationType("ADD_INSTRUMENT");

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void addHpanDateSameLastEndActiveInterval(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.with(LocalTime.MIN).plusDays(1L);
        LocalDateTime end = now.plusDays(2L).with(LocalTime.MAX);
        ActiveTimeInterval initialActiveInterval = ActiveTimeInterval.builder().startInterval(start).endInterval(end).build();
        List<ActiveTimeInterval> activeList = new ArrayList<>();
        activeList.add(initialActiveInterval);

        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID")
                .status("ACTIVE")
                .activeTimeIntervals(activeList).build();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(end);
        hpanUpdateEvaluateDTO.setOperationType("ADD_INSTRUMENT");

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getLastEndInterval());

        List<ActiveTimeInterval> activeIntervalsListResult = result.getActiveTimeIntervals();
        Assertions.assertEquals(2, activeIntervalsListResult.size());
        ActiveTimeInterval newActiveIntervalExpected = ActiveTimeInterval.builder().startInterval(end.with(LocalTime.MIN).plusDays(1L)).build();
        Assertions.assertTrue(activeIntervalsListResult.contains(newActiveIntervalExpected));
        Assertions.assertEquals(activeIntervalsListResult, List.of(initialActiveInterval, newActiveIntervalExpected));
    }
}
