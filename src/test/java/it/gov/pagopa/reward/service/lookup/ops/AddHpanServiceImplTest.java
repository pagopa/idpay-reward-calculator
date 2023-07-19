package it.gov.pagopa.reward.service.lookup.ops;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.BaseOnboardingInfo;
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
        LocalDateTime time = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        int activeIntervalsInitial = hpanInitiatives.getOnboardedInitiatives().size();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT);

        // When
         OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO, BaseOnboardingInfo.builder().initiativeId("INITIATIVE_%d".formatted(bias)).build());

        // Then
        Assertions.assertNotNull(result);

        Assertions.assertEquals(hpanUpdateEvaluateDTO.getEvaluationDate(), result.getUpdateDate());
        List<ActiveTimeInterval> activeTimeIntervals = result.getActiveTimeIntervals();
        Assertions.assertEquals(3,activeTimeIntervals.size());
        Assertions.assertTrue(activeTimeIntervals.contains(ActiveTimeInterval.builder().startInterval(time).build()));
        Assertions.assertNotEquals(activeIntervalsInitial,activeTimeIntervals.size());

        Assertions.assertNull(result.getLastEndInterval());
    }

    @Test
    void addHpanAfterLasIntervalOpen(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);

        String initiativeId = "INITIATIVE_%d".formatted(bias);
        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId(initiativeId);
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT);

        //When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO, BaseOnboardingInfo.builder().initiativeId("INITIATIVE_%d".formatted(bias)).build());
        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(hpanUpdateEvaluateDTO.getEvaluationDate(), result.getUpdateDate());

        List<ActiveTimeInterval> activeTimeIntervalsResult = result.getActiveTimeIntervals();
        Assertions.assertEquals(3, activeTimeIntervalsResult.size());

        ActiveTimeInterval intervalChangeExpected = ActiveTimeInterval.builder()
                .startInterval(time.minusMonths(5L))
                .endInterval(time).build();
        Assertions.assertTrue(activeTimeIntervalsResult.contains(intervalChangeExpected));

        ActiveTimeInterval newIntervalExpected = ActiveTimeInterval.builder()
                .startInterval(time).build();
        Assertions.assertTrue(activeTimeIntervalsResult.contains(newIntervalExpected));
        Assertions.assertNull(result.getLastEndInterval());
    }

    @Test
    void addHpanIntoLastIntervalClose() {
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().minusMonths(6L).with(LocalTime.MIN).plusDays(1L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT);

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO, BaseOnboardingInfo.builder().initiativeId("INITIATIVE_%d".formatted(bias)).build());
        // Then
        Assertions.assertNull(result);
    }

    @Test
    void addHpanBeforeLastActiveInterval(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO1.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO1.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO1.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO1.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO1.setEvaluationDate(time.minusYears(4L));
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO2 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO2.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO2.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO2.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO2.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO2.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO2.setEvaluationDate(time.minusYears(1L).minusMonths(3L));
        hpanUpdateEvaluateDTO2.setOperationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT);

        // When
        OnboardedInitiative resultBeforeAllActiveIntervals = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO1, BaseOnboardingInfo.builder().initiativeId("INITIATIVE_%d".formatted(bias)).build());
        OnboardedInitiative resultBeforeLastActiveIntervalsAndAfterAnyInterval = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO2, BaseOnboardingInfo.builder().initiativeId("INITIATIVE_%d".formatted(bias)).build());

        // Then
        Assertions.assertNull(resultBeforeAllActiveIntervals);
        Assertions.assertNull(resultBeforeLastActiveIntervalsAndAfterAnyInterval);
    }

    @Test
    void addHpanWithNewInitiative(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        String newInitiativeId = String.format("ANOTHER_INITIATIVE_%d",bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId(newInitiativeId);
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT);

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO, BaseOnboardingInfo.builder().initiativeId("INITIATIVE_%d".formatted(bias)).build());
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getLastEndInterval());
        Assertions.assertEquals(hpanUpdateEvaluateDTO.getEvaluationDate(), result.getUpdateDate());
    }

    @Test
    void addHpanWithNoneInitiative(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .hpan("HPAN_%d".formatted(bias))
                .userId("USERID_%d".formatted(bias)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId("First_INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT);

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO, BaseOnboardingInfo.builder().initiativeId("First_INITIATIVE_%d".formatted(bias)).build());
        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getLastEndInterval());
        Assertions.assertEquals(hpanUpdateEvaluateDTO.getEvaluationDate(), result.getUpdateDate());
    }

    @Test
    void unexpectedNotMaxActiveInterval(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();

        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID")
                .status(HpanInitiativeStatus.ACTIVE)
                .activeTimeIntervals(new ArrayList<>()).build();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now().with(LocalTime.MIN).plusDays(1L));
        hpanUpdateEvaluateDTO.setOperationType("ADD_INSTRUMENT");

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO, BaseOnboardingInfo.builder().initiativeId("INITIATIVEID").build());

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void addHpanDateSameLastEndActiveInterval(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();

        LocalDateTime now = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);

        ActiveTimeInterval initialActiveInterval = ActiveTimeInterval.builder().startInterval(now.minusMonths(5L)).endInterval(now).build();
        List<ActiveTimeInterval> activeList = new ArrayList<>();
        activeList.add(initialActiveInterval);

        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID")
                .status(HpanInitiativeStatus.ACTIVE)
                .activeTimeIntervals(activeList).build();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(now);
        hpanUpdateEvaluateDTO.setOperationType("ADD_INSTRUMENT");

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO, BaseOnboardingInfo.builder().initiativeId("INITIATIVEID").build());

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getLastEndInterval());
        Assertions.assertEquals(hpanUpdateEvaluateDTO.getEvaluationDate(), result.getUpdateDate());

        List<ActiveTimeInterval> activeIntervalsListResult = result.getActiveTimeIntervals();
        Assertions.assertEquals(2, activeIntervalsListResult.size());
        ActiveTimeInterval newActiveIntervalExpected = ActiveTimeInterval.builder().startInterval(now).build();
        Assertions.assertTrue(activeIntervalsListResult.contains(newActiveIntervalExpected));
        Assertions.assertEquals(activeIntervalsListResult, List.of(initialActiveInterval, newActiveIntervalExpected));
    }

    @Test
    void unexpectedStatus(){
        // Given
        AddHpanService addHpanService = new AddHpanServiceImpl();

        LocalDateTime now = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);

        ActiveTimeInterval initialActiveInterval = ActiveTimeInterval.builder().startInterval(now.minusMonths(5L)).endInterval(now.minusDays(1L)).build();
        List<ActiveTimeInterval> activeList = new ArrayList<>();
        activeList.add(initialActiveInterval);

        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID")
                .status(HpanInitiativeStatus.INACTIVE)
                .activeTimeIntervals(activeList).build();

        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan("HPAN")
                .onboardedInitiatives(List.of(onboarded)).build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = HpanUpdateEvaluateDTO.builder()
                .hpan(hpanInitiatives.getHpan())
                .maskedPan(hpanInitiatives.getMaskedPan())
                .brandLogo(hpanInitiatives.getBrandLogo())
                .brand(hpanInitiatives.getBrand())
                .initiativeId("INITIATIVEID")
                .userId(hpanInitiatives.getUserId())
                .evaluationDate(now)
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .build();

        // When
        OnboardedInitiative result = addHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO, BaseOnboardingInfo.builder().initiativeId("INITIATIVEID").build());

        // Then
        Assertions.assertNull(result);
    }
}
