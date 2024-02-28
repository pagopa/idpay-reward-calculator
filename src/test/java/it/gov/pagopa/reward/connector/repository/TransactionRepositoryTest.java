package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.mongo.MongoTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.Map;

@MongoTest
@ContextConfiguration(classes = {TransactionRepositoryImpl.class})
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Test
    void test(){
        String trxId = "TRXID";
        assertResult(trxId, false);
        mongoTemplate.save(new HashMap<>(Map.of("_id", trxId)), "transaction").block();
        assertResult(trxId, true);
        deleteDocument(trxId);
        assertResult(trxId, false);
    }

    private void deleteDocument(String trxId) {
        mongoTemplate.remove(new Query(Criteria.where("_id").is(trxId)), "transaction").block();
    }

    private void assertResult(String trxId, boolean expected) {
        Assertions.assertEquals(expected, repository.checkIfExists(trxId).block());
    }
}
