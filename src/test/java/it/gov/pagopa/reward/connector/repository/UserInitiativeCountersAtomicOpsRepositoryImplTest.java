package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import org.bson.BsonString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MongoTest
class UserInitiativeCountersAtomicOpsRepositoryImplTest {
    @Autowired
    protected UserInitiativeCountersRepository userInitiativeCountersRepository;

    protected String userId = "userId";
    protected String initiativeId = "initiativeId";
    protected String userCounterId = UserInitiativeCounters.buildId(userId,initiativeId);

    @AfterEach
    void clearData(){
        userInitiativeCountersRepository.deleteById(userCounterId).block();
    }

    private void storeTestCounter() {
        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
        userInitiativeCounters.setUpdateDate(LocalDateTime.now().minusMinutes(5));
        userInitiativeCountersRepository.save(userInitiativeCounters).block();
    }

    @Test
    void testCreateIfNotExists(){
        UserInitiativeCounters expectedCounter = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);

        Assertions.assertEquals(Boolean.FALSE, userInitiativeCountersRepository.existsById(expectedCounter.getId()).block());

        UpdateResult createResult = userInitiativeCountersRepository.createIfNotExists(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId).block();
        Assertions.assertEquals(UpdateResult.acknowledged(0, 0L, new BsonString(expectedCounter.getId())), createResult);

        UserInitiativeCounters stored = userInitiativeCountersRepository.findById(expectedCounter.getId()).block();
        Assertions.assertNotNull(stored);
        Assertions.assertFalse(stored.getUpdateDate().isBefore(expectedCounter.getUpdateDate()));
        Assertions.assertFalse(stored.getUpdateDate().isAfter(LocalDateTime.now()));
        expectedCounter.setUpdateDate(stored.getUpdateDate());

        Assertions.assertEquals(expectedCounter, stored);

        UpdateResult updateResult = userInitiativeCountersRepository.createIfNotExists(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId).block();
        Assertions.assertEquals(UpdateResult.acknowledged(1, 0L, null), updateResult);
    }

    @Test
    void findByInitiativesWithBatch() {
        storeTestCounter();

        Flux<UserInitiativeCounters> result = userInitiativeCountersRepository.findByInitiativesWithBatch(initiativeId, 100);

        List<UserInitiativeCounters> userInitiativeCounters = result.toStream().toList();
        assertEquals(1, userInitiativeCounters.size());
    }

    @Test
    void unlockPendingTrx(){
        // When
        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
        userInitiativeCounters.setUpdateDate(LocalDateTime.now().minusMinutes(5));

        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setInitiatives(List.of(initiativeId));
        trx.setUserId(userId);

        userInitiativeCounters.setPendingTrx(trx);
        UserInitiativeCounters storedBefore = userInitiativeCountersRepository.save(userInitiativeCounters).block();

        // Where
        UserInitiativeCounters updateResult = userInitiativeCountersRepository.unlockPendingTrx(trx.getId()).block();

        assertNotNull(updateResult);
        assertNull(updateResult.getPendingTrx());

        assertNotNull(storedBefore);
        LocalDateTime updateDateBefore = storedBefore.getUpdateDate();
        assertNotNull(updateDateBefore);
        assertTrue(updateDateBefore.isBefore(updateResult.getUpdateDate()));

    }
}