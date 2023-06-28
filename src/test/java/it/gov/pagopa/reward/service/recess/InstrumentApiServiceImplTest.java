package it.gov.pagopa.reward.service.recess;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.service.lookup.HpanInitiativesService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class InstrumentApiServiceImplTest {

    private static final String USERID = "USERID";
    private static final String INITIATIVEID = "INITIATIVEID";
    private static final String HPAN = "HPAN";
    @Mock private HpanInitiativesRepository hpanInitiativesRepositoryMock;
    @Mock private HpanInitiativesService hpanInitiativesServiceMock;


    private InstrumentApiServiceImpl instrumentApiService;

    @BeforeEach
    void setUp() {
        instrumentApiService= new InstrumentApiServiceImpl(hpanInitiativesRepositoryMock, hpanInitiativesServiceMock);
    }

    @Test
    void cancelInstrument() {
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder().userId(USERID)
                .hpan(HPAN).build();

        Mockito.doReturn(Flux.fromIterable(List.of(hpanInitiatives))).when(hpanInitiativesRepositoryMock)
                        .retrieveAHpanByUserIdAndInitiativeIdAndStatus(Mockito.eq(USERID), Mockito.eq(INITIATIVEID), Mockito.any(), Mockito.any());

        OnboardedInitiative oi = OnboardedInitiative.builder().initiativeId(INITIATIVEID).build();
        Mockito.doReturn(oi)
                .when(hpanInitiativesServiceMock).evaluate(Mockito.any(), Mockito.any(), Mockito.anyBoolean());

        UpdateResult ur = Mockito.mock(UpdateResult.class);
        Mockito.doReturn(Mono.just(ur)).when(hpanInitiativesRepositoryMock).setInitiative(HPAN,oi);

        Void result = instrumentApiService.cancelInstruments(USERID, INITIATIVEID).block();

        Assertions.assertNull(result);
    }
}