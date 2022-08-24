package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2EntityMapper;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

class HpanInitiativesServiceImplTest {

    @Test
    void addHpanAfterLastIntervalClose() {
        // Given
        HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper = Mockito.mock(HpanInitiativeDTO2EntityMapper.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(hpanInitiativeDTO2EntityMapper);

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        int activeIntervalsInitial = hpanInitiatives.getOnboardedInitiatives().size();

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time);
        hpanInitiativeDTO.setOperationType(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name());

        // When
        HpanInitiatives result = hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO, Mono.just(hpanInitiatives))).block();

        // Then
        Assertions.assertNotNull(result);

        List<OnboardedInitiative> onboardedInitiativeList = result.getOnboardedInitiatives()
                .stream().filter(o -> o.getInitiativeId().equals("INITIATIVE_%d".formatted(bias))).toList();
        Assertions.assertEquals(1,onboardedInitiativeList.size());

        List<ActiveTimeInterval> activeTimeIntervals = onboardedInitiativeList.get(0).getActiveTimeIntervals();
        Assertions.assertEquals(3,activeTimeIntervals.size());
        Assertions.assertTrue(activeTimeIntervals.contains(ActiveTimeInterval.builder().startInterval(time).build()));
        Assertions.assertNotEquals(activeIntervalsInitial,activeTimeIntervals.size());
    }

    @Test
    void addHpanAfterLastActiveInterval(){
        // Given
        HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper = Mockito.mock(HpanInitiativeDTO2EntityMapper.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(hpanInitiativeDTO2EntityMapper);

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);

        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO1.setOperationDate(time.minusYears(4L));
        hpanInitiativeDTO1.setOperationType(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name());

        HpanInitiativeDTO hpanInitiativeDTO2 = new HpanInitiativeDTO();
        hpanInitiativeDTO2.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO2.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO2.setOperationDate(time.minusYears(1L).minusMonths(3L));
        hpanInitiativeDTO2.setOperationType(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name());

        // When
        Mono<HpanInitiatives> resultBeforeAllActiveIntervals = hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO1, Mono.just(hpanInitiatives)));
        Mono<HpanInitiatives> resultBeforeLastActiveIntervalsAndAfterAnyInterval = hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO2, Mono.just(hpanInitiatives)));

        // Then
        Assertions.assertEquals(Boolean.FALSE, resultBeforeAllActiveIntervals.hasElement().block());
        Assertions.assertEquals(Boolean.FALSE, resultBeforeLastActiveIntervalsAndAfterAnyInterval.hasElement().block());
    }

    @Test
    void addHpanWithInitiativeNotValid(){
        // Given
        HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper = Mockito.mock(HpanInitiativeDTO2EntityMapper.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(hpanInitiativeDTO2EntityMapper);

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        int activeIntervalsInitial = hpanInitiatives.getOnboardedInitiatives().size();

        HpanInitiativeDTO hpanInitiativeDTO = new HpanInitiativeDTO();
        hpanInitiativeDTO.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO.setInitiativeId("ANOTHER_INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO.setOperationDate(time);
        hpanInitiativeDTO.setOperationType(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name());

        // When
        HpanInitiatives result = hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO, Mono.just(hpanInitiatives))).block();

        // Then
        Assertions.assertNull(result);
//        Assertions.assertNotEquals(hpanInitiatives.getOnboardedInitiatives().size(),result.getOnboardedInitiatives().size());
    }

    @Test
    void deleteHpanBeforeLastActiveInterval(){
        // Given
        HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper = Mockito.mock(HpanInitiativeDTO2EntityMapper.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(hpanInitiativeDTO2EntityMapper);

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithCloseIntervals(bias);
        int activeIntervalsInitial = hpanInitiatives.getOnboardedInitiatives().size();

        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO1.setOperationDate(time.minusYears(4L));
        hpanInitiativeDTO1.setOperationType(HpanInitiativeDTO.OperationType.DELETE_INSTRUMENT.name());

        HpanInitiativeDTO hpanInitiativeDTO2 = new HpanInitiativeDTO();
        hpanInitiativeDTO2.setHpan(hpanInitiatives.getHpan());
        hpanInitiativeDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO2.setUserId(hpanInitiatives.getUserId());
        hpanInitiativeDTO2.setOperationDate(time.minusYears(1L).minusMonths(3L));
        hpanInitiativeDTO2.setOperationType(HpanInitiativeDTO.OperationType.DELETE_INSTRUMENT.name());

        //When
        Mono<HpanInitiatives> result1 = hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO1, Mono.just(hpanInitiatives)));
        Mono<HpanInitiatives> result2= hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO2, Mono.just(hpanInitiatives)));

        //Then
        Assertions.assertEquals(Boolean.FALSE, result1.hasElement().block());
        Assertions.assertEquals(Boolean.FALSE, result2.hasElement().block());


    }

    @Test
    void deleteHpanAfterLastActiveIntervalNotClose(){
        // Given
        HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper = Mockito.mock(HpanInitiativeDTO2EntityMapper.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(hpanInitiativeDTO2EntityMapper);

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
        hpanInitiativeDTO1.setOperationType(HpanInitiativeDTO.OperationType.DELETE_INSTRUMENT.name());

        //When
        HpanInitiatives result = hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO1, Mono.just(hpanInitiatives))).block();

        //Then
        Assertions.assertNotNull(result);

        ActiveTimeInterval activeTimeIntervalAfter = result.getOnboardedInitiatives().get(0).getActiveTimeIntervals().stream().filter(o -> o.getStartInterval().equals(activeTimeIntervalsInitials.getStartInterval())).toList().get(0);
        Assertions.assertEquals(time,activeTimeIntervalAfter.getEndInterval());
        Assertions.assertNotEquals(activeTimeIntervalsInitials,activeTimeIntervalAfter);

    }

    @Test
    void anyOperationHpanNotPresent(){
        // Given
        HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper = Mockito.mock(HpanInitiativeDTO2EntityMapper.class);
        HpanInitiativesService hpanInitiativesService = new HpanInitiativesServiceImpl(hpanInitiativeDTO2EntityMapper);

        int bias = 1;
        LocalDateTime time = LocalDateTime.now();

        HpanInitiativeDTO hpanInitiativeDTO1 = new HpanInitiativeDTO();
        hpanInitiativeDTO1.setHpan("HPAN_"+bias);
        hpanInitiativeDTO1.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO1.setUserId("USERID_"+bias);
        hpanInitiativeDTO1.setOperationDate(time);
        hpanInitiativeDTO1.setOperationType(HpanInitiativeDTO.OperationType.DELETE_INSTRUMENT.name());

        HpanInitiativeDTO hpanInitiativeDTO2 = new HpanInitiativeDTO();
        hpanInitiativeDTO2.setHpan("HPAN_"+bias);
        hpanInitiativeDTO2.setInitiativeId("INITIATIVE_%d".formatted(bias));
        hpanInitiativeDTO2.setUserId("USERID_"+bias);
        hpanInitiativeDTO2.setOperationDate(time);
        hpanInitiativeDTO2.setOperationType(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name());

        // When
        Mono<HpanInitiatives> result1 = hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO1, Mono.empty()));
        Mono<HpanInitiatives> result2 = hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO2, Mono.empty()));

        // Then
        Assertions.assertEquals(Boolean.FALSE,result1.hasElement().block());
        Assertions.assertEquals(Boolean.FALSE,result2.hasElement().block());

    }
}