package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersAtomicOpsRepository;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersAtomicOpsRepositoryImpl;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Temporary Class, to fix integrated tests
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
public class UserInitativeCountersAtomicOpsRepositoryUnitTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    private UserInitiativeCountersAtomicOpsRepository userInitiativeCountersAtomicOpsRepository;

    @BeforeEach
    public void init() {
        userInitiativeCountersAtomicOpsRepository = new UserInitiativeCountersAtomicOpsRepositoryImpl(mongoTemplate);
    }

    @Test
    public void testUpdateInitativeStatusOK() {
        userInitiativeCountersAtomicOpsRepository.updateEntityIdByInitiativeIdAndEntityId(
                "INITATIVEID","ENTITYID", "NEWENTITYID");
        verify(mongoTemplate).findAndModify(any(),any(),any());
    }


}
