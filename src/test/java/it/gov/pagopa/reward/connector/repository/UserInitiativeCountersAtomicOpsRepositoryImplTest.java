package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.bson.BsonString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class UserInitiativeCountersAtomicOpsRepositoryImplTest extends BaseIntegrationTest {

    @Autowired
    protected UserInitiativeCountersRepository userInitiativeCountersRepository;
    @Value("${app.synchronousTransactions.throttlingSeconds}")
    private long throttlingSeconds;

    protected String userId = "userId";
    protected String initiativeId = "initiativeId";
    protected String userCounterId = UserInitiativeCounters.buildId(userId,initiativeId);

    @AfterEach
    void clearData(){
        userInitiativeCountersRepository.deleteById(userCounterId).block();
    }

    @Test
    void testFindByIdThrottled(){
        String trxId = "TRXID";
        UserInitiativeCounters notFoundResult = userInitiativeCountersRepository.findByIdThrottled("DUMMYID", trxId).block();
        Assertions.assertNull(notFoundResult);

        storeTestCounter();

        UserInitiativeCounters stored = checkFindByIdThrottled(trxId, trxId);

        String userCounterIdStored = stored.getId();
        Assertions.assertEquals(userCounterId,userCounterIdStored);

        Mono<UserInitiativeCounters> mono = userInitiativeCountersRepository.findByIdThrottled(userCounterId, trxId);
        try{
            mono.block();
            Assertions.fail("Expected exception");
        } catch (RuntimeException e){
            if (e instanceof  ClientExceptionNoBody exceptionNoBody) {
                Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, exceptionNoBody.getHttpStatus());
            }else {
                Assertions.fail("Expected ClientExceptionNoBody");
            }
        }

        TestUtils.wait(throttlingSeconds, TimeUnit.SECONDS);

        checkFindByIdThrottled("TRXID2", trxId);

    }

    @Test
    void testFindByIdThrottled_concurrent() {
        int N = 20;
        AtomicInteger dropped = new AtomicInteger(0);

        storeTestCounter();

        MongoTestUtilitiesService.startMongoCommandListener("findByIdThrottled_Concurrent");
        Long successfulLocks = Flux.fromStream(IntStream.range(0, N).boxed())
                .flatMap(x -> userInitiativeCountersRepository.findByIdThrottled(userCounterId, "TRXID")
                        .onErrorResume(ClientExceptionNoBody.class, e -> {
                            Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
                            dropped.incrementAndGet();
                            return Mono.empty();
                        }))
                .count()
                .block();
        List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>> commands = MongoTestUtilitiesService.stopAndGetMongoCommands();
        MongoTestUtilitiesService.printMongoCommands(commands);

        Assertions.assertEquals(1, successfulLocks);
        Assertions.assertEquals(N - 1, dropped.get());

        Map<String, List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>>> groupByCommand = commands.stream().collect(Collectors.groupingBy(c -> c.getKey().getType()));
        Assertions.assertEquals(N, groupByCommand.get("findAndModify").get(0).getValue());
        Assertions.assertEquals(N - 1, groupByCommand.get("find").get(0).getValue());
    }

    private UserInitiativeCounters storeTestCounter() {
        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
        userInitiativeCounters.setUpdateDate(LocalDateTime.now().minusMinutes(5));
        return userInitiativeCountersRepository.save(userInitiativeCounters).block();
    }

    private UserInitiativeCounters checkFindByIdThrottled(String trxId, String expectedTrxId) {
        return checkSyncThrottledOp(trxId, expectedTrxId, userInitiativeCountersRepository::findByIdThrottled);
    }

    private UserInitiativeCounters checkSyncThrottledOp(String trxId, String expectedTrxId, BiFunction<String, String, Mono<UserInitiativeCounters>> op) {
        LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        UserInitiativeCounters stored = op.apply(userCounterId, trxId).block();
        LocalDateTime after = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        Assertions.assertNotNull(stored);
        Assertions.assertEquals(expectedTrxId!=null? List.of(expectedTrxId) : null, stored.getUpdatingTrxId());
        Assertions.assertFalse(stored.getUpdateDate().isBefore(before));
        Assertions.assertFalse(stored.getUpdateDate().isAfter(after));
        return stored;
    }

    @Test
    void testSetUpdatingTrxId(){
        String updatedTrxId = "TRXID2";
        UserInitiativeCounters userInitiativeCounters = storeTestCounter();
        Assertions.assertTrue(CollectionUtils.isEmpty(userInitiativeCounters.getUpdatingTrxId()));
        checkSyncThrottledOp(updatedTrxId, updatedTrxId, userInitiativeCountersRepository::setUpdatingTrx);
        checkSyncThrottledOp(null, null, userInitiativeCountersRepository::setUpdatingTrx);
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
}