//package it.gov.pagopa.reward.connector.repository;
//
//import com.mongodb.client.result.UpdateResult;
//import it.gov.pagopa.common.mongo.MongoTest;
//import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
//import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
//import it.gov.pagopa.reward.exception.custom.InvalidCounterVersionException;
//import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
//import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
//import org.bson.BsonString;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//
//import static it.gov.pagopa.reward.utils.RewardConstants.REWARD_STATE_REJECTED;
//import static it.gov.pagopa.reward.utils.RewardConstants.TRX_CHANNEL_BARCODE;
//import static org.junit.jupiter.api.Assertions.*;
//
//@MongoTest
//class UserInitiativeCountersAtomicOpsRepositoryImplTest {
//    @Autowired
//    protected UserInitiativeCountersRepository userInitiativeCountersRepository;
//
//    protected String userId = "userId";
//    protected String initiativeId = "initiativeId";
//    protected String userCounterId = UserInitiativeCounters.buildId(userId,initiativeId);
//
//    @AfterEach
//    void clearData(){
//        userInitiativeCountersRepository.deleteById(userCounterId).block();
//    }
//
//    private void storeTestCounter() {
//        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
//        userInitiativeCounters.setUpdateDate(LocalDateTime.now().minusMinutes(5));
//        userInitiativeCountersRepository.save(userInitiativeCounters).block();
//    }
//
//    @Test
//    void testCreateIfNotExists(){
//        UserInitiativeCounters expectedCounter = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
//
//        Assertions.assertEquals(Boolean.FALSE, userInitiativeCountersRepository.existsById(expectedCounter.getId()).block());
//
//        UpdateResult createResult = userInitiativeCountersRepository.createIfNotExists(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId).block();
//        Assertions.assertEquals(UpdateResult.acknowledged(0, 0L, new BsonString(expectedCounter.getId())), createResult);
//
//        UserInitiativeCounters stored = userInitiativeCountersRepository.findById(expectedCounter.getId()).block();
//        Assertions.assertNotNull(stored);
//        Assertions.assertFalse(stored.getUpdateDate().isBefore(expectedCounter.getUpdateDate()));
//        Assertions.assertFalse(stored.getUpdateDate().isAfter(LocalDateTime.now()));
//        expectedCounter.setUpdateDate(stored.getUpdateDate());
//
//        Assertions.assertEquals(expectedCounter, stored);
//
//        UpdateResult updateResult = userInitiativeCountersRepository.createIfNotExists(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId).block();
//        Assertions.assertEquals(UpdateResult.acknowledged(1, 0L, null), updateResult);
//    }
//
//    @Test
//    void findByInitiativesWithBatch() {
//        storeTestCounter();
//
//        Flux<UserInitiativeCounters> result = userInitiativeCountersRepository.findByInitiativesWithBatch(initiativeId, 100);
//
//        List<UserInitiativeCounters> userInitiativeCounters = result.toStream().toList();
//        assertEquals(1, userInitiativeCounters.size());
//    }
//
//    @Test
//    void unlockPendingTrx(){
//        // When
//        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
//        userInitiativeCounters.setUpdateDate(LocalDateTime.now().minusMinutes(5));
//
//        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
//        trx.setInitiatives(List.of(initiativeId));
//        trx.setUserId(userId);
//
//        userInitiativeCounters.setPendingTrx(trx);
//        UserInitiativeCounters storedBefore = userInitiativeCountersRepository.save(userInitiativeCounters).block();
//
//        // Where
//        UserInitiativeCounters updateResult = userInitiativeCountersRepository.unlockPendingTrx(trx.getId()).block();
//
//        assertNotNull(updateResult);
//        assertNull(updateResult.getPendingTrx());
//
//        assertNotNull(storedBefore);
//        LocalDateTime updateDateBefore = storedBefore.getUpdateDate();
//        assertNotNull(updateDateBefore);
//        assertTrue(updateDateBefore.isBefore(updateResult.getUpdateDate()));
//
//    }
//
//    @Test
//    void findByPendingTrx(){
//        // When
//        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
//        userInitiativeCounters.setUpdateDate(LocalDateTime.now().minusMinutes(5));
//
//        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
//        trx.setInitiatives(List.of(initiativeId));
//        trx.setUserId(userId);
//        trx.setChannel(TRX_CHANNEL_BARCODE);
//        trx.setStatus(REWARD_STATE_REJECTED);
//
//        userInitiativeCounters.setPendingTrx(trx);
//
//        userInitiativeCountersRepository.save(userInitiativeCounters).block();
//
//        // Where
//        UserInitiativeCounters updateResult = userInitiativeCountersRepository.findByPendingTrx(trx.getId()).block();
//
//        assertNotNull(updateResult);
//        assertNotNull(updateResult.getPendingTrx());
//
//    }
//
//    @Test
//    void saveIfVersionNotChanged(){
//        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
//        userInitiativeCounters.setVersion(1L);
//
//        UserInitiativeCounters userInitiativeCountersUpdate = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
//        userInitiativeCountersUpdate.setVersion(2L);
//        userInitiativeCountersUpdate.setTrxNumber(1L);
//        userInitiativeCountersUpdate.setTotalRewardCents(1_00L);
//        userInitiativeCountersUpdate.setTotalAmountCents(1_00L);
//        userInitiativeCountersUpdate.setUpdateDate(userInitiativeCountersUpdate.getUpdateDate().truncatedTo(ChronoUnit.MILLIS));
//
//        userInitiativeCountersRepository.save(userInitiativeCounters).block();
//
//        //where
//        UserInitiativeCounters result = userInitiativeCountersRepository.saveIfVersionNotChanged(userInitiativeCountersUpdate).block();
//
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals(userInitiativeCountersUpdate, result);
//
//    }
//
//    @Test
//    void saveIfVersionNotChangedError(){
//        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
//        userInitiativeCounters.setVersion(2L);
//
//        UserInitiativeCounters userInitiativeCountersUpdate = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
//        userInitiativeCountersUpdate.setVersion(2L);
//        userInitiativeCounters.setTotalRewardCents(1_00L);
//
//        userInitiativeCountersRepository.save(userInitiativeCounters).block();
//
//        //where
//        Mono<UserInitiativeCounters> serviceResult = userInitiativeCountersRepository.saveIfVersionNotChanged(userInitiativeCountersUpdate);
//        InvalidCounterVersionException result = assertThrows(InvalidCounterVersionException.class, serviceResult::block);
//
//        Assertions.assertNotNull(result);
//
//    }
//}