package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class UserInitiativeCountersAtomicOpsRepositoryImpl implements UserInitiativeCountersAtomicOpsRepository{
    private final int throttlingSeconds;
    private final ReactiveMongoTemplate mongoTemplate;

    public UserInitiativeCountersAtomicOpsRepositoryImpl(@Value("${app.synchronousTransactions.throttlingSeconds}") int throttlingSeconds, ReactiveMongoTemplate mongoTemplate) {
        this.throttlingSeconds = throttlingSeconds;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<UserInitiativeCounters> findByIdThrottled(String id) {
        return mongoTemplate
                .findAndModify(
                        Query.query(criteriaById(id)
                                .orOperator(
                                        Criteria.where(UserInitiativeCounters.Fields.updateDate).is(null),
                                        Criteria.where(UserInitiativeCounters.Fields.updateDate).is(LocalDateTime.now().minusSeconds(throttlingSeconds)))),
                        new Update()
                                .setOnInsert(UserInitiativeCounters.Fields.updateDate, LocalDateTime.now()),
                        UserInitiativeCounters.class
                )
                .switchIfEmpty(mongoTemplate.exists(Query.query(criteriaById(id)), UserInitiativeCounters.class)
                        .mapNotNull(counterExist -> {
                            if(Boolean.TRUE.equals(counterExist)){
                                throw new ClientExceptionNoBody(HttpStatus.TOO_MANY_REQUESTS,"MANY_REQUESTS");
                            } else {
                                return null;
                            }
                        }));
    }

    private Criteria criteriaById(String id) {
        return Criteria
                .where(UserInitiativeCounters.Fields.id).is(id);
    }

}
