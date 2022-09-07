package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OnboardedInitiativesServiceImplTest {

    @Test
    void getInitiativesWhenNoEndEnd(){
        getInitiatives(null, null, true);
    }

    @Test
    void getInitiativesWhenFutureEndEnd(){
        getInitiatives(LocalDate.now().plusDays(11), OffsetDateTime.now().plusDays(10), true);
    }

    @Test
    void getInitiativesWhenPastEndEnd(){
        getInitiatives(LocalDate.now().plusDays(9), OffsetDateTime.now().plusDays(10), false);
    }

    void getInitiatives(LocalDate initiativeEndDate, OffsetDateTime trxDateTime, boolean expectSuccess) {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        RewardContextHolderService rewardContextHolderService = Mockito.mock(RewardContextHolderService.class);
        OnboardedInitiativesService onboardedInitiativesService = new OnboardedInitiativesServiceImpl(hpanInitiativesRepository, rewardContextHolderService);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";

        if(trxDateTime == null){
            trxDateTime = OffsetDateTime.now().plusDays(10L);
        }

        Integer bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);
        log.info(trxDateTime.atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime().toString());
        log.info(hpanInitiatives.toString());

        Mockito.when(hpanInitiativesRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.just(hpanInitiatives));

        final String mockedInitiativeId = hpanInitiatives.getOnboardedInitiatives().get(0).getInitiativeId();
        Mockito.when(rewardContextHolderService.getInitiativeConfig(Mockito.eq(mockedInitiativeId))).thenReturn(
                InitiativeConfig.builder()
                        .initiativeId(mockedInitiativeId)
                        .endDate(initiativeEndDate)
                        .build());

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(hpanMock, trxDateTime).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        if(expectSuccess) {
            Mockito.verify(hpanInitiativesRepository).findById(Mockito.same(hpanMock));
            Assertions.assertEquals(1, result.size());
            Assertions.assertTrue(result.contains(String.format("INITIATIVE_%d", bias)));
        } else {
            checkInitiativeNotFound(hpanInitiativesRepository, hpanMock, result);
        }
    }

    @Test
    void getInitiativesHpanNotValid() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        RewardContextHolderService rewardContextHolderService = Mockito.mock(RewardContextHolderService.class);
        OnboardedInitiativesService onboardedInitiativesService = new OnboardedInitiativesServiceImpl(hpanInitiativesRepository, rewardContextHolderService);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(6L);


        Mockito.when(hpanInitiativesRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.empty());

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(hpanMock, trxDateMock).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(hpanInitiativesRepository, hpanMock, result);
    }

    private void checkInitiativeNotFound(HpanInitiativesRepository hpanInitiativesRepository, String hpanMock, List<String> result) {
        Mockito.verify(hpanInitiativesRepository).findById(Mockito.same(hpanMock));
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getNotOnboardedInitiatives() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        RewardContextHolderService rewardContextHolderService = Mockito.mock(RewardContextHolderService.class);
        OnboardedInitiativesService onboardedInitiativesService = new OnboardedInitiativesServiceImpl(hpanInitiativesRepository, rewardContextHolderService);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(6L);

        Integer bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(bias);

        Mockito.when(hpanInitiativesRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.just(hpanInitiatives));

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(hpanMock, trxDateMock).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(hpanInitiativesRepository, hpanMock, result);
    }

    @Test
    void getNotIntitiaveTrxNotInActiveInterval() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        RewardContextHolderService rewardContextHolderService = Mockito.mock(RewardContextHolderService.class);
        OnboardedInitiativesService onboardedInitiativesService = new OnboardedInitiativesServiceImpl(hpanInitiativesRepository, rewardContextHolderService);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(6L);

        Integer bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceNotInActiveInterval(bias);

        Mockito.when(hpanInitiativesRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.just(hpanInitiatives));

        final String mockedInitiativeId = hpanInitiatives.getOnboardedInitiatives().get(0).getInitiativeId();
        Mockito.when(rewardContextHolderService.getInitiativeConfig(Mockito.eq(mockedInitiativeId))).thenReturn(InitiativeConfig.builder().initiativeId(mockedInitiativeId).build());

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(hpanMock, trxDateMock).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(hpanInitiativesRepository, hpanMock, result);
    }
}