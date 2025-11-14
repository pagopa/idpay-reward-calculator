package it.gov.pagopa.reward.service.recess;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.connector.repository.secondary.HpanInitiativesRepository;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class InstrumentApiServiceImplTest {

    private static final String USERID = "USERID";
    private static final String INITIATIVEID = "INITIATIVEID";
    @Mock private HpanInitiativesRepository hpanInitiativesRepositoryMock;
    @Mock private UserInitiativeCountersRepository userInitiativeCountersRepository;


    private InstrumentApiServiceImpl instrumentApiService;

    @BeforeEach
    void setUp() {
        instrumentApiService= new InstrumentApiServiceImpl(userInitiativeCountersRepository, hpanInitiativesRepositoryMock);
    }

    @Test
    void cancelInstrument() {

        HpanInitiatives hpanInitiatives =
                HpanInitiatives.builder()
                .onboardedInitiatives(
                        Collections.singletonList(
                                OnboardedInitiative.builder().familyId("FAMILYID").initiativeId(INITIATIVEID).build()))
                .userId(USERID).build();
        Mockito.doReturn(Mono.empty()).when(userInitiativeCountersRepository).updateEntityIdByInitiativeIdAndEntityId(
                eq(INITIATIVEID), eq("FAMILYID"),eq("HISTORY_FAMILYID"));
        Mockito.doReturn(Mono.just(hpanInitiatives))
                .when(hpanInitiativesRepositoryMock).setUserInitiativeStatus(USERID,INITIATIVEID, HpanInitiativeStatus.INACTIVE);

        Void result = instrumentApiService.disableUserInitiativeInstruments(USERID, INITIATIVEID).block();

        Assertions.assertNull(result);
    }
    @Test
    void reactivateInstrument() {

        HpanInitiatives hpanInitiatives =
                HpanInitiatives.builder()
                        .onboardedInitiatives(
                                Collections.singletonList(
                                        OnboardedInitiative.builder().familyId("FAMILYID").initiativeId(INITIATIVEID).build()))
                        .userId(USERID).build();
        Mockito.doReturn(Mono.empty()).when(userInitiativeCountersRepository).updateEntityIdByInitiativeIdAndEntityId(
                eq(INITIATIVEID), eq("HISTORY_FAMILYID"),eq("FAMILYID"));
        Mockito.doReturn(Mono.just(hpanInitiatives))
                .when(hpanInitiativesRepositoryMock).setUserInitiativeStatus(USERID,INITIATIVEID, HpanInitiativeStatus.ACTIVE);

        Void result = instrumentApiService.enableUserInitiativeInstruments(USERID, INITIATIVEID).block();

        Assertions.assertNull(result);
    }
}