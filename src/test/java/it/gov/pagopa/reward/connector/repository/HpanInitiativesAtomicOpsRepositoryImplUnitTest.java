package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.connector.repository.secondary.HpanInitiativesAtomicOpsRepository;
import it.gov.pagopa.reward.connector.repository.secondary.HpanInitiativesAtomicOpsRepositoryImpl;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Temporary Class, to fix integrated tests
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class HpanInitiativesAtomicOpsRepositoryImplUnitTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    private HpanInitiativesAtomicOpsRepository hpanInitiativesAtomicOpsRepository;

    @BeforeEach
    public void init() {
        hpanInitiativesAtomicOpsRepository = new HpanInitiativesAtomicOpsRepositoryImpl(mongoTemplate);
    }

    @Test
    public void testUpdateInitativeStatusOK() {
        hpanInitiativesAtomicOpsRepository.setUserInitiativeStatus(
                "USERID","INITIATIVEID", HpanInitiativeStatus.INACTIVE);
        verify(mongoTemplate).findAndModify(any(),any(),any());
    }


}
