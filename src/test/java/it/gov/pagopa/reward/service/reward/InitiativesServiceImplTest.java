package it.gov.pagopa.reward.service.reward;

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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
class InitiativesServiceImplTest {

    @Test
    void getInitiatives() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        InitiativesService initiativesService = new InitiativesServiceImpl(hpanInitiativesRepository);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(10L);

        Integer bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);
        log.info(trxDateMock.atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime().toString());
        log.info(hpanInitiatives.toString());

        Mockito.when(hpanInitiativesRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.just(hpanInitiatives));

        // When
        List<String> result = initiativesService.getInitiatives(hpanMock, trxDateMock);

        // Then
        Mockito.verify(hpanInitiativesRepository).findById(Mockito.same(hpanMock));
        Assertions.assertEquals(1,result.size());
        Assertions.assertTrue(result.contains(String.format("INITIATIVE_%d",bias)));
    }

    @Test
    void getInitiativesHpanNotValid() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        InitiativesService initiativesService = new InitiativesServiceImpl(hpanInitiativesRepository);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(6L);


        Mockito.when(hpanInitiativesRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.empty());

        // When
        List<String> result = initiativesService.getInitiatives(hpanMock, trxDateMock);

        // Then
        Mockito.verify(hpanInitiativesRepository).findById(Mockito.same(hpanMock));
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getNotOnboardedInitiatives() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        InitiativesService initiativesService = new InitiativesServiceImpl(hpanInitiativesRepository);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(6L);

        Integer bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(bias);

        Mockito.when(hpanInitiativesRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.just(hpanInitiatives));

        // When
        List<String> result = initiativesService.getInitiatives(hpanMock, trxDateMock);

        // Then
        Mockito.verify(hpanInitiativesRepository).findById(Mockito.same(hpanMock));
        Assertions.assertTrue(result.isEmpty());
    }
}