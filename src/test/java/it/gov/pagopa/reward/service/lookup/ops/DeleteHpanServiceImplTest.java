package it.gov.pagopa.reward.service.lookup.ops;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
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
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO2 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO2.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO2.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO2.setEvaluationDate(time.minusYears(1L).minusMonths(3L));
        hpanUpdateEvaluateDTO2.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

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
        LocalDateTime time = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO1.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO1.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO1.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO1.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO1.setEvaluationDate(time);
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

        //When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO1);

        //Then
        Assertions.assertNotNull(result);

        Assertions.assertEquals(hpanUpdateEvaluateDTO1.getEvaluationDate(), result.getUpdateDate());

        List<ActiveTimeInterval> activeTimeIntervalsResult = result.getActiveTimeIntervals();
        Assertions.assertEquals(2, activeTimeIntervalsResult.size());

        ActiveTimeInterval activeIntervalExpected = ActiveTimeInterval.builder()
                .startInterval(time.minusMonths(5L).plusDays(1))
                .endInterval(time).build();

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
        LocalDateTime time = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L).plusMonths(2L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO1 = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO1.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO1.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO1.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO1.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO1.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO1.setEvaluationDate(time);
        hpanUpdateEvaluateDTO1.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

        //When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO1);

        //Then
        Assertions.assertNull(result);
    }

    @Test
    void deleteHpanPresentWithNotInitiativeId() {
        // Given
        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L).with(LocalTime.MIN).plusDays(1L);
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId("ANOTHER_INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

        //When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        //Then
        Assertions.assertNull(result);
    }

    @Test
    void deleteHpanIntoLastActiveIntervalClose() {
        // Given
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
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void deleteHpanOpenToday(){
        // Given
        int bias = 1;
        LocalDateTime time = LocalDateTime.now();
        LocalDateTime timeStartInterval = time.minusMinutes(5L);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(String.format("INITIATIVE_%d",bias))
                .activeTimeIntervals(new ArrayList<>())
                .build();
        ActiveTimeInterval interval1 = ActiveTimeInterval.builder().startInterval(timeStartInterval).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval1);

        List<OnboardedInitiative> onboardedInitiativeList = new ArrayList<>();
        onboardedInitiativeList.add(onboardedInitiative);
        hpanInitiatives.setOnboardedInitiatives(onboardedInitiativeList);


        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setHpan(hpanInitiatives.getHpan());
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(time);
        hpanUpdateEvaluateDTO.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(hpanUpdateEvaluateDTO.getEvaluationDate(), result.getUpdateDate());
        Assertions.assertEquals(1, result.getActiveTimeIntervals().size());
        ActiveTimeInterval activeTimeInterval = result.getActiveTimeIntervals().get(0);
        Assertions.assertEquals(timeStartInterval,activeTimeInterval.getStartInterval());
        Assertions.assertEquals(time,activeTimeInterval.getEndInterval());
    }

    @Test
    void hpanNull() {
        // Given
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder().userId("USERID").build();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = new HpanUpdateEvaluateDTO();
        hpanUpdateEvaluateDTO.setInitiativeId("ANOTHER_INITIATIVE");
        hpanUpdateEvaluateDTO.setUserId("USERID");
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now().with(LocalTime.MIN).plusDays(1L));
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
        hpanUpdateEvaluateDTO.setMaskedPan(hpanInitiatives.getMaskedPan());
        hpanUpdateEvaluateDTO.setBrandLogo(hpanInitiatives.getBrandLogo());
        hpanUpdateEvaluateDTO.setBrand(hpanInitiatives.getBrand());
        hpanUpdateEvaluateDTO.setInitiativeId("INITIATIVEID");
        hpanUpdateEvaluateDTO.setUserId(hpanInitiatives.getUserId());
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now().with(LocalTime.MIN).plusDays(1L));
        hpanUpdateEvaluateDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void unexpectedInitiativeWithoutIntervals(){
        // Given
        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID").status(HpanInitiativeStatus.ACTIVE).build();
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
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now().with(LocalTime.MIN).plusDays(1L));
        hpanUpdateEvaluateDTO.setOperationType("DELETE_INSTRUMENT");

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void unexpectedEmptyActiveInterval(){
        // Given
        OnboardedInitiative onboarded = OnboardedInitiative.builder().initiativeId("INITIATIVEID").status(HpanInitiativeStatus.ACTIVE).activeTimeIntervals(new ArrayList<>()).build();
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
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now().with(LocalTime.MIN).plusDays(1L));
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
                .status(HpanInitiativeStatus.ACTIVE)
                .activeTimeIntervals(new ArrayList<>()).build();

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
        hpanUpdateEvaluateDTO.setEvaluationDate(LocalDateTime.now().with(LocalTime.MIN).plusDays(1L));
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

        LocalDateTime elabDateTime = LocalDateTime.now().plusDays(1).with(LocalTime.MIN);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        hpanInitiatives.getOnboardedInitiatives().get(0)
                .getActiveTimeIntervals()
                .add(new ActiveTimeInterval(elabDateTime, null));

        hpanInitiatives.getOnboardedInitiatives().get(0).getActiveTimeIntervals().forEach(System.out::println);

        HpanUpdateEvaluateDTO request = new HpanUpdateEvaluateDTO();
        request.setHpan(hpanInitiatives.getHpan());
        request.setMaskedPan(hpanInitiatives.getMaskedPan());
        request.setBrandLogo(hpanInitiatives.getBrandLogo());
        request.setBrand(hpanInitiatives.getBrand());
        request.setInitiativeId(initiativeId);
        request.setUserId(hpanInitiatives.getUserId());
        request.setEvaluationDate(elabDateTime);
        request.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

        // When
        OnboardedInitiative result1 = deleteHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(request.getEvaluationDate(), result1.getUpdateDate());
        Assertions.assertEquals(2, result1.getActiveTimeIntervals().size());
        ActiveTimeInterval lastInterval= result1.getActiveTimeIntervals().get(result1.getActiveTimeIntervals().size() - 1);
        Assertions.assertNotNull(lastInterval.getEndInterval());
        Assertions.assertEquals(lastInterval.getEndInterval(), result1.getLastEndInterval());

        // When adding again
        AddHpanService addHpanService = new AddHpanServiceImpl();
        OnboardedInitiative result2 = addHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(request.getEvaluationDate(), result2.getUpdateDate());
        Assertions.assertEquals(3, result2.getActiveTimeIntervals().size());
        ActiveTimeInterval lastInterval2 = result2.getActiveTimeIntervals().get(result2.getActiveTimeIntervals().size() - 1);
        Assertions.assertEquals(elabDateTime, lastInterval2.getStartInterval());
        Assertions.assertNull(lastInterval2.getEndInterval());
        Assertions.assertNull(result2.getLastEndInterval());

        // When deleting again
        OnboardedInitiative result3 = deleteHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(request.getEvaluationDate(), result2.getUpdateDate());
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

        LocalDateTime elabDateTime = LocalDateTime.now().plusDays(1).with(LocalTime.MIN);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(bias);

        HpanUpdateEvaluateDTO request = new HpanUpdateEvaluateDTO();
        request.setHpan(hpanInitiatives.getHpan());
        request.setMaskedPan(hpanInitiatives.getMaskedPan());
        request.setBrandLogo(hpanInitiatives.getBrandLogo());
        request.setBrand(hpanInitiatives.getBrand());
        request.setInitiativeId(initiativeId);
        request.setUserId(hpanInitiatives.getUserId());
        request.setEvaluationDate(elabDateTime);
        request.setOperationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT);

        // When adding first interval
        AddHpanService addHpanService = new AddHpanServiceImpl();
        OnboardedInitiative firstAddResult = addHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(1, firstAddResult.getActiveTimeIntervals().size());
        ActiveTimeInterval interval = firstAddResult.getActiveTimeIntervals().get(0);
        Assertions.assertEquals(elabDateTime, interval.getStartInterval());
        Assertions.assertNull(interval.getEndInterval());
        hpanInitiatives.setOnboardedInitiatives(new ArrayList<>(List.of(firstAddResult)));

        // When deleting it
        OnboardedInitiative result1 = deleteHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertNotNull(result1);
        Assertions.assertEquals(request.getEvaluationDate(), result1.getUpdateDate());
        Assertions.assertEquals(0, result1.getActiveTimeIntervals().size());
        hpanInitiatives.getOnboardedInitiatives().remove(firstAddResult);

        // When adding again
        OnboardedInitiative result2 = addHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(request.getEvaluationDate(), result2.getUpdateDate());
        Assertions.assertEquals(1, result2.getActiveTimeIntervals().size());
        ActiveTimeInterval lastInterval2 = result2.getActiveTimeIntervals().get(result2.getActiveTimeIntervals().size() - 1);
        Assertions.assertEquals(elabDateTime, lastInterval2.getStartInterval());
        Assertions.assertNull(lastInterval2.getEndInterval());

        // When deleting again
        OnboardedInitiative result3 = deleteHpanService.execute(hpanInitiatives, request);

        // Then
        Assertions.assertEquals(request.getEvaluationDate(), result3.getUpdateDate());
        Assertions.assertEquals(0, result3.getActiveTimeIntervals().size());
    }

    @Test
    void unexpectedStatus(){
        // Given
        DeleteHpanService deleteHpanService = new DeleteHpanServiceImpl();

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
                .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                .build();

        // When
        OnboardedInitiative result = deleteHpanService.execute(hpanInitiatives, hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }
}