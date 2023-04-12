package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.dto.mapper.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.exception.TransactionSynchronousException;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public class UserInitiativeCountersAtomicOpsRepositoryImpl implements UserInitiativeCountersAtomicOpsRepository{
    private final int throttlingSeconds;
    private final ReactiveMongoTemplate mongoTemplate;
    private final SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;

    public UserInitiativeCountersAtomicOpsRepositoryImpl(@Value("${app.synchronous.throttlingSeconds}") int ThrottlingSeconds, ReactiveMongoTemplate mongoTemplate, SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper) {
        this.throttlingSeconds = ThrottlingSeconds;
        this.mongoTemplate = mongoTemplate;
        this.synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper = synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
    }

    @Override
    public Mono<UserInitiativeCounters> updateDate(SynchronousTransactionRequestDTO request, String initiativeId) {
        Mono<UserInitiativeCounters> initiativeCountersMono = mongoTemplate
                .findAndModify(
                        Query.query(criteriaByUserIdAndInitiativeId(request.getUserId(), initiativeId)
                                .orOperator(
                                        Criteria.where(UserInitiativeCounters.Fields.updateDate).is(null),
                                        Criteria.where(UserInitiativeCounters.Fields.updateDate).is(LocalDateTime.now().minusSeconds(throttlingSeconds)))),
                        new Update()
                                .setOnInsert(UserInitiativeCounters.Fields.updateDate, LocalDateTime.now()),
                        UserInitiativeCounters.class
                );

        initiativeCountersMono.hasElement().filter(b ->b.equals(Boolean.FALSE))
                .flatMap(b -> mongoTemplate.exists(Query.query(criteriaByUserIdAndInitiativeId(request.getUserId(), initiativeId)), UserInitiativeCounters.class))
                .map(b -> {
                    if(b.equals(Boolean.TRUE)){
                        throw new TransactionSynchronousException(synchronousTransactionRequestDTOt2TrxDtoOrResponseMapper.apply(request, initiativeId, List.of(RewardConstants.TRX_REJECTION_USER_NOT_ENABLED)));
                    }
                    return b;
                });

        return initiativeCountersMono;
    }

    private Criteria criteriaByUserIdAndInitiativeId(String userId, String initiativeId) {
        return Criteria
                .where(UserInitiativeCounters.Fields.userId).is(userId)
                .and(UserInitiativeCounters.Fields.initiativeId).is(initiativeId);
    }

}
