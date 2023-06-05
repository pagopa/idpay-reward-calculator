package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public class UserInitiativeCountersAtomicOpsRepositoryImpl implements UserInitiativeCountersAtomicOpsRepository {
    private final int throttlingSeconds;
    private final ReactiveMongoTemplate mongoTemplate;

    public UserInitiativeCountersAtomicOpsRepositoryImpl(
            @Value("${app.synchronousTransactions.throttlingSeconds:1}") int throttlingSeconds,

            ReactiveMongoTemplate mongoTemplate) {
        this.throttlingSeconds = throttlingSeconds;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<UserInitiativeCounters> findByIdThrottled(String id, String updatingTrxId) {
        return mongoTemplate
                .findAndModify(
                        Query.query(criteriaById(id)
                                .orOperator(
                                        Criteria.where(UserInitiativeCounters.Fields.updateDate).is(null),
                                        Criteria.where(UserInitiativeCounters.Fields.updateDate).lt(LocalDateTime.now().minusSeconds(throttlingSeconds)))),
                        new Update()
                                .currentDate(UserInitiativeCounters.Fields.updateDate)
                                .push(UserInitiativeCounters.Fields.updatingTrxId).slice(1).each(updatingTrxId),
                        FindAndModifyOptions.options().returnNew(true),
                        UserInitiativeCounters.class
                )
                .switchIfEmpty(mongoTemplate.exists(Query.query(criteriaById(id)), UserInitiativeCounters.class)
                        .mapNotNull(counterExist -> {
                            if (Boolean.TRUE.equals(counterExist)) {
                                throw new ClientExceptionNoBody(HttpStatus.TOO_MANY_REQUESTS, "MANY_REQUESTS");
                            } else {
                                return null;
                            }
                        }));
    }

    private Criteria criteriaById(String id) {
        return Criteria
                .where(UserInitiativeCounters.Fields.id).is(id);
    }

    @Override
    public Mono<UserInitiativeCounters> setUpdatingTrx(String id, String updatingTrxId) {
        return mongoTemplate
                .findAndModify(
                        Query.query(criteriaById(id)),
                        new Update()
                                .currentDate(UserInitiativeCounters.Fields.updateDate)
                                .set(UserInitiativeCounters.Fields.updatingTrxId, List.of(updatingTrxId)),
                        FindAndModifyOptions.options().returnNew(true),
                        UserInitiativeCounters.class
                );
    }

}
