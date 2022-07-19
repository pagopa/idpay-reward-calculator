package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.model.CitizenHpan;
import it.gov.pagopa.reward.repository.CitizenHpanRepository;
import it.gov.pagopa.reward.test.fakers.CitizenHpanFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
class InitiativesServiceImplTest {

    @Test
    void getInitiatives() {
        // Given
        CitizenHpanRepository citizenHpanRepository = Mockito.mock(CitizenHpanRepository.class);
        InitiativesService initiativesService = new InitiativesServiceImpl(citizenHpanRepository);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(6L);

        Integer bias = 1;
        CitizenHpan citizenHpan = CitizenHpanFaker.mockInstance(bias);

        Mockito.when(citizenHpanRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.just(citizenHpan));

        // When
        List<String> result = initiativesService.getInitiatives(hpanMock, trxDateMock);

        // Then
        Mockito.verify(citizenHpanRepository).findById(Mockito.same(hpanMock));
        Assertions.assertEquals(1,result.size());
        Assertions.assertTrue(result.contains(String.format("INITIATIVE_%d",bias)));
    }

    @Test
    void getInitiativesHpanNotValid() {
        // Given
        CitizenHpanRepository citizenHpanRepository = Mockito.mock(CitizenHpanRepository.class);
        InitiativesService initiativesService = new InitiativesServiceImpl(citizenHpanRepository);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(6L);


        Mockito.when(citizenHpanRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.empty());

        // When
        List<String> result = initiativesService.getInitiatives(hpanMock, trxDateMock);

        // Then
        Mockito.verify(citizenHpanRepository).findById(Mockito.same(hpanMock));
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getNotOnboardedInitiatives() {
        // Given
        CitizenHpanRepository citizenHpanRepository = Mockito.mock(CitizenHpanRepository.class);
        InitiativesService initiativesService = new InitiativesServiceImpl(citizenHpanRepository);

        String hpanMock = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
        OffsetDateTime trxDateMock = OffsetDateTime.now().plusDays(6L);

        Integer bias = 1;
        CitizenHpan citizenHpan = CitizenHpanFaker.mockInstanceWithoutInitiative(bias);

        Mockito.when(citizenHpanRepository.findById(Mockito.same(hpanMock))).thenReturn(Mono.just(citizenHpan));

        // When
        List<String> result = initiativesService.getInitiatives(hpanMock, trxDateMock);

        // Then
        Mockito.verify(citizenHpanRepository).findById(Mockito.same(hpanMock));
        Assertions.assertTrue(result.isEmpty());
    }
}