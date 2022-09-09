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

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time);
        hpanInitiativeDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        HpanInitiatives result = addHpanService.execute(hpanInitiatives, hpanInitiativeDTO);

        // Then
        Assertions.assertNotNull(result);

        List<OnboardedInitiative> onboardedInitiativeList = result.getOnboardedInitiatives()
                .stream().filter(o -> o.getInitiativeId().equals("INITIATIVE_%d".formatted(bias))).toList();
        Assertions.assertEquals(1,onboardedInitiativeList.size());

        List<ActiveTimeInterval> activeTimeIntervals = onboardedInitiativeList.get(0).getActiveTimeIntervals();
        Assertions.assertEquals(3,activeTimeIntervals.size());
        Assertions.assertTrue(activeTimeIntervals.contains(ActiveTimeInterval.builder().startInterval(time.with(LocalTime.MIN).plusDays(1L)).build()));
        Assertions.assertNotEquals(activeIntervalsInitial,activeTimeIntervals.size());
    }

    @Test
    void addHpanAfterLasIntervalOpen(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);


        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time.plusMonths(2L));
        hpanInitiativeDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        //When
        HpanInitiatives result = addHpanService.execute(hpanInitiatives, hpanInitiativeDTO);
        //Then
        Assertions.assertNotNull(result);

        List<ActiveTimeInterval> activeTimeIntervalsResult = result.getOnboardedInitiatives().get(0).getActiveTimeIntervals();
        Assertions.assertEquals(3, activeTimeIntervalsResult.size());
        System.out.println(activeTimeIntervalsResult);

        ActiveTimeInterval intervalChangeExpected = ActiveTimeInterval.builder().startInterval(time.plusDays(5L).with(LocalTime.MIN).plusDays(1)).endInterval(time.plusMonths(2L).with(LocalTime.MAX)).build();
        Assertions.assertTrue(activeTimeIntervalsResult.contains(intervalChangeExpected));

        ActiveTimeInterval newIntervalExpected = ActiveTimeInterval.builder().startInterval(time.plusMonths(2L).with(LocalTime.MIN).plusDays(1)).build();
        Assertions.assertTrue(activeTimeIntervalsResult.contains(newIntervalExpected));

    }

    @Test
    void addHpanIntoLastIntervalClose() {
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().minusMonths(6L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time);
        hpanInitiativeDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        HpanInitiatives result = addHpanService.execute(hpanInitiatives, hpanInitiativeDTO);
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

        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO1.setOperationDate(time.minusYears(4L));
        hpanInitiativeDTO1.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        HpanInitiativeDTO hpanInitiativeDTO2 = new HpanInitiativeDTO();
        hpanInitiativeDTO2.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO2.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO2.setOperationDate(time.minusYears(1L).minusMonths(3L));
        hpanInitiativeDTO2.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        HpanInitiatives resultBeforeAllActiveIntervals = addHpanService.execute(hpanInitiatives, hpanInitiativeDTO1);
        HpanInitiatives resultBeforeLastActiveIntervalsAndAfterAnyInterval = addHpanService.execute(hpanInitiatives, hpanInitiativeDTO2);

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

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId(newInitiativeId);
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time);
        hpanInitiativeDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        HpanInitiatives result = addHpanService.execute(hpanInitiatives, hpanInitiativeDTO);
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2,result.getOnboardedInitiatives().size());

        List<String> initiativesOnboarded = result.getOnboardedInitiatives().stream().map(OnboardedInitiative::getInitiativeId).toList();
        Assertions.assertTrue(initiativesOnboarded.contains(newInitiativeId));
        Assertions.assertEquals(List.of(String.format("INITIATIVE_%d",bias), newInitiativeId),initiativesOnboarded);
    }

    @Test
    void addHpanWithNoOneInitiative(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .hpan("HPAN_%d".formatted(bias))
                .userId("USERID_%d".formatted(bias)).build();

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("First_INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time);
        hpanInitiativeDTO.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        // When
        HpanInitiatives result = addHpanService.execute(hpanInitiatives, hpanInitiativeDTO);
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1,result.getOnboardedInitiatives().size());
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

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVEID");
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(LocalDateTime.now());
        hpanInitiativeDTO.setOperationType("ADD_INSTRUMENT");

        // When
        HpanInitiatives result = addHpanService.execute(hpanInitiatives, hpanInitiativeDTO);

        // Then
        Assertions.assertNull(result);
    }
}