package it.gov.pagopa.reward.service.recess;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class InstrumentApiServiceImplTest {

    private static final String USERID = "USERID";
    private static final String INITIATIVEID = "INITIATIVEID";
    @Mock private HpanInitiativesRepository hpanInitiativesRepositoryMock;


    private InstrumentApiServiceImpl instrumentApiService;

    @BeforeEach
    void setUp() {
        instrumentApiService= new InstrumentApiServiceImpl(hpanInitiativesRepositoryMock);
    }

    @Test
    void cancelInstrument() {

        UpdateResult ur = Mockito.mock(UpdateResult.class);
        Mockito.doReturn(Mono.just(ur))
                .when(hpanInitiativesRepositoryMock).setIfNotEqualsStatus(USERID,INITIATIVEID, HpanInitiativeStatus.INACTIVE);

        Void result = instrumentApiService.cancelInstruments(USERID, INITIATIVEID).block();

        Assertions.assertNull(result);
    }
    @Test
    void reactivateInstrument() {

        UpdateResult ur = Mockito.mock(UpdateResult.class);
        Mockito.doReturn(Mono.just(ur))
                .when(hpanInitiativesRepositoryMock).setIfNotEqualsStatus(USERID,INITIATIVEID, HpanInitiativeStatus.ACTIVE);

        Void result = instrumentApiService.reactivateInstruments(USERID, INITIATIVEID).block();

        Assertions.assertNull(result);
    }
}