package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2InitialEntityMapper;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

class HpanInitiativesServiceImplTest {

    @Test
    void addHpanAfterLastIntervalClose() {
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

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
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now().plusMonths(2L);
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);

        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO1.setOperationDate(time);
        hpanInitiativeDTO1.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);

        //When
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO1, hpanInitiatives);

        //Then
        Assertions.assertNull(result);
    }

    @Test
    void addHpanIntoLastIntervalClose() {
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

        // Then
        Assertions.assertNull(result);
    }
    @Test
    void addHpanBeforeLastActiveInterval(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives resultBeforeAllActiveIntervals = hpanInitiativesService.evaluate(hpanInitiativeDTO1, hpanInitiatives);
        HpanInitiatives resultBeforeLastActiveIntervalsAndAfterAnyInterval = hpanInitiativesService.evaluate(hpanInitiativeDTO2, hpanInitiatives);

        // Then
        Assertions.assertNull(resultBeforeAllActiveIntervals);
        Assertions.assertNull(resultBeforeLastActiveIntervalsAndAfterAnyInterval);
    }

    @Test
    void addHpanWithNewInitiative(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

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
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1,result.getOnboardedInitiatives().size());
    }

    @Test
    void deleteHpanBeforeLastActiveInterval(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result1 = hpanInitiativesService.evaluate(hpanInitiativeDTO1, hpanInitiatives);
        HpanInitiatives result2= hpanInitiativesService.evaluate(hpanInitiativeDTO2, hpanInitiatives);

        //Then
        Assertions.assertNull(result1);
        Assertions.assertNull(result2);
    }

    @Test
    void deleteHpanAfterLastActiveIntervalNotClose(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO1, hpanInitiatives);

        //Then
        Assertions.assertNotNull(result);

        ActiveTimeInterval activeTimeIntervalAfter = result.getOnboardedInitiatives().get(0).getActiveTimeIntervals().stream().filter(o -> o.getStartInterval().equals(activeTimeIntervalsInitials.getStartInterval())).toList().get(0);
        Assertions.assertEquals(time.with(LocalTime.MAX),activeTimeIntervalAfter.getEndInterval());
        Assertions.assertNotEquals(activeTimeIntervalsInitials,activeTimeIntervalAfter);
        Assertions.assertEquals(activeTimeIntervalsInitials.getStartInterval(),activeTimeIntervalAfter.getStartInterval());
        Assertions.assertNotEquals(activeTimeIntervalsInitials.getEndInterval(),activeTimeIntervalAfter.getEndInterval());
    }

    @Test
    void deleteHpanAfterLastActiveIntervalClose(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO1, hpanInitiatives);

        //Then
        Assertions.assertNotNull(result);

        ActiveTimeInterval activeTimeIntervalAfter = result.getOnboardedInitiatives().get(0).getActiveTimeIntervals().stream().filter(o -> o.getStartInterval().equals(lastActive)).toList().get(0);
        Assertions.assertEquals(time.with(LocalTime.MAX),activeTimeIntervalAfter.getEndInterval());
        Assertions.assertNotEquals(lastActiveIntervalInitials,activeTimeIntervalAfter);
        Assertions.assertEquals(lastActiveIntervalInitials.getStartInterval(),activeTimeIntervalAfter.getStartInterval());
        Assertions.assertNotEquals(lastActiveIntervalInitials.getEndInterval(),activeTimeIntervalAfter.getEndInterval());
    }

    @Test
    void deleteHpanPresentWithNotInitiativeId(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

        //Then
        Assertions.assertNull(result);
    }

    @Test
    void deleteHpanIntoLastActiveIntervalClose(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();

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
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

        // Then
        Assertions.assertNull(result);
    }
    @Test
    void anyOperationHpanNotPresent(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();
        HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper = new HpanInitiativeDTO2InitialEntityMapper();

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan("HPAN_"+bias);
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId("USERID_"+bias);
        hpanInitiativeDTO1.setOperationDate(time);
        hpanInitiativeDTO1.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);

        HpanInitiativeDTO hpanInitiativeDTO2 = new HpanInitiativeDTO();
        hpanInitiativeDTO2.setHpan("HPAN_"+bias);
        hpanInitiativeDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO2.setUserId("USERID_"+bias);
        hpanInitiativeDTO2.setOperationDate(time);
        hpanInitiativeDTO2.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);
        HpanInitiatives hpanInitiatives1 = hpanInitiativeDTO2InitialEntityMapper.apply(hpanInitiativeDTO1);
        HpanInitiatives hpanInitiatives2 = hpanInitiativeDTO2InitialEntityMapper.apply(hpanInitiativeDTO2);

        // When
        HpanInitiatives result1 = hpanInitiativesService.evaluate(hpanInitiativeDTO1, hpanInitiatives1);
        HpanInitiatives result2 = hpanInitiativesService.evaluate(hpanInitiativeDTO2, hpanInitiatives2);

        // Then
        Assertions.assertNull(result1);
        Assertions.assertNotNull(result2);
        TestUtils.checkNotNullFields(result2,"acceptanceDate");
    }

    @Test
    void invalidOperation(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();
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

    @Test
    void hpanNull(){
        // Given
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl();
        HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper = new HpanInitiativeDTO2InitialEntityMapper();


        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setInitiativeId("ANOTHER_INITIATIVE");
        hpanInitiativeDTO.setUserId("USERID");
        hpanInitiativeDTO.setOperationDate(LocalDateTime.now());
        hpanInitiativeDTO.setOperationType("DELETE_INSTRUMENT");

        HpanInitiatives hpanInitiatives = hpanInitiativeDTO2InitialEntityMapper.apply(hpanInitiativeDTO);
        // When
        HpanInitiatives result = hpanInitiativesService.evaluate(hpanInitiativeDTO, hpanInitiatives);

        // Then
        Assertions.assertNull(result);
    }
}