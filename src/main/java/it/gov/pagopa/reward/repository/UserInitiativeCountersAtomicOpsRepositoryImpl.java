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
    public Mono<UserInitiativeCounters> findByThrottled(String id) {
        Mono<UserInitiativeCounters> initiativeCountersMono = mongoTemplate
                .findAndModify(
                        Query.query(criteriaById(id)
                                .orOperator(
                                        Criteria.where(UserInitiativeCounters.Fields.updateDate).is(null),
                                        Criteria.where(UserInitiativeCounters.Fields.updateDate).is(LocalDateTime.now().minusSeconds(throttlingSeconds)))),
                        new Update()
                                .setOnInsert(UserInitiativeCounters.Fields.updateDate, LocalDateTime.now()),
                        UserInitiativeCounters.class
                );

        return initiativeCountersMono.hasElement().filter(b ->b.equals(Boolean.FALSE))
                .flatMap(b -> mongoTemplate.exists(Query.query(criteriaById(id)), UserInitiativeCounters.class))
                .flatMap(b -> {
                    if(b.equals(Boolean.TRUE)){
                        throw new ClientExceptionNoBody(HttpStatus.TOO_MANY_REQUESTS);
                    }
                    return initiativeCountersMono;
                });
    }

    private Criteria criteriaById(String id) {
        return Criteria
                .where(UserInitiativeCounters.Fields.id).is(id);
    }

}
