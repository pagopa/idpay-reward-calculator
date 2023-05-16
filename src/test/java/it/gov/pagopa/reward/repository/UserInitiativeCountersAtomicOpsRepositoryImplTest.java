package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

class UserInitiativeCountersAtomicOpsRepositoryImplTest extends BaseIntegrationTest {
    @Autowired
    protected UserInitiativeCountersRepository userInitiativeCountersRepository;

    protected String userId = "userId";
    protected String initiativeId = "initiativeId";
    protected String userCounterId = UserInitiativeCounters.buildId(userId,initiativeId);

    @AfterEach
    void clearData(){
        userInitiativeCountersRepository.deleteById(userCounterId).block();
    }

    @Test
    void testFindByIdThrottled(){
        UserInitiativeCounters notFoundResult = userInitiativeCountersRepository.findByIdThrottled("DUMMYID").block();
        Assertions.assertNull(notFoundResult);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters("userId", "initiativeId");
        userInitiativeCounters.setUpdateDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        UserInitiativeCounters stored = userInitiativeCountersRepository.save(userInitiativeCounters).block();
        Assertions.assertNotNull(stored);
        String userCounterIdStored = stored.getId();
        Assertions.assertEquals(userCounterId,userCounterIdStored);

        try{
            userInitiativeCountersRepository.findByIdThrottled(userCounterId).block();
            Assertions.fail("Expected exception");
        } catch (RuntimeException e){
            if (e instanceof  ClientExceptionNoBody exceptionNoBody) {
                Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, exceptionNoBody.getHttpStatus());
            }else {
                Assertions.fail("Expected ClientExceptionNoBody");
            }
        }
    }

}