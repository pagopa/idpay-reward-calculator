
package it.gov.pagopa.reward.repository.primary;

import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersAtomicOpsRepositoryImpl;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserInitiativeCountersAtomicOpsRepositoryImplTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    private UserInitiativeCountersAtomicOpsRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new UserInitiativeCountersAtomicOpsRepositoryImpl(mongoTemplate);
    }

    @Test
    void unlockPendingTrx_shouldReturnUpdatedCounters() {
        String trxId = "trx123";
        UserInitiativeCounters mockCounters = new UserInitiativeCounters();
        mockCounters.setId("counterId");
        mockCounters.setPendingTrx(null);

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(UserInitiativeCounters.class)))
                .thenReturn(Mono.just(mockCounters));

        StepVerifier.create(repository.unlockPendingTrx(trxId))
                .expectNextMatches(c -> c.getId().equals("counterId") && c.getPendingTrx() == null)
                .verifyComplete();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findAndModify(queryCaptor.capture(), any(), any(), eq(UserInitiativeCounters.class));
        assertEquals(trxId, queryCaptor.getValue().getQueryObject().get("pendingTrx.id"));

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(any(), updateCaptor.capture(), any(), eq(UserInitiativeCounters.class));
        assertNull(updateCaptor.getValue().getUpdateObject().get("$set", Document.class).get("pendingTrx"));
    }

    @Test
    void unlockPendingTrx_shouldReturnEmptyIfNotFound() {
        when(mongoTemplate.findAndModify(any(), any(), any(), eq(UserInitiativeCounters.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(repository.unlockPendingTrx("trxNotFound"))
                .verifyComplete();
    }

    @Test
    void unlockPendingTrxById_shouldReturnUpdatedCounters() {
        String id = "counterId";
        UserInitiativeCounters mockCounters = new UserInitiativeCounters();
        mockCounters.setId(id);
        mockCounters.setPendingTrx(null);

        when(mongoTemplate.findAndModify(any(), any(), any(), eq(UserInitiativeCounters.class)))
                .thenReturn(Mono.just(mockCounters));

        StepVerifier.create(repository.unlockPendingTrxById(id))
                .expectNextMatches(c -> c.getId().equals(id) && c.getPendingTrx() == null)
                .verifyComplete();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).findAndModify(queryCaptor.capture(), any(), any(), eq(UserInitiativeCounters.class));
        assertEquals(id, queryCaptor.getValue().getQueryObject().get("_id"));

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).findAndModify(any(), updateCaptor.capture(), any(), eq(UserInitiativeCounters.class));
        assertNull(updateCaptor.getValue().getUpdateObject().get("$set", Document.class).get("pendingTrx"));
    }

    @Test
    void unlockPendingTrxById_shouldReturnEmptyIfNotFound() {
        when(mongoTemplate.findAndModify(any(), any(), any(), eq(UserInitiativeCounters.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(repository.unlockPendingTrxById("idNotFound"))
                .verifyComplete();
    }
}
