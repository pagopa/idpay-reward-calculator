package it.gov.pagopa.reward.service.synchronous.op.recover;

import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class HandleSyncCounterUpdatingTrxServiceTest {

    @Mock private TransactionProcessedRepository transactionProcessedRepositoryMock;
    @Mock private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;

    private HandleSyncCounterUpdatingTrxService service;

    private TransactionDTO trx;
    private UserInitiativeCounters counters;

    @BeforeEach
    void init(){
        service = new HandleSyncCounterUpdatingTrxServiceImpl(transactionProcessedRepositoryMock, userInitiativeCountersRepositoryMock);

        trx = TransactionDTOFaker.mockInstance(0);
        counters = new UserInitiativeCounters(trx.getUserId(), "INITIATIVEID");
    }

    private void checkResult(UserInitiativeCounters result) {
        Assertions.assertNotNull(result);
        Assertions.assertSame(counters, result);
        Assertions.assertNull(result.getUpdatingTrxId());
    }

    @Test
    void testSameUpdatingTrx(){
        // Given
        counters.setUpdatingTrxId(List.of(trx.getId()));

        // When
        UserInitiativeCounters result = service.checkUpdatingTrx(trx, counters).block();

        // Then
        checkResult(result);
    }

    // region testNoUpdatingTrx
    @Test
    void testNoUpdatingTrx_nullCollection(){
        testNoUpdatingTrx(null);
    }
    @Test
    void testNoUpdatingTrx_emptyCollection(){
        testNoUpdatingTrx(Collections.emptyList());
    }
    void testNoUpdatingTrx(List<String> emptyOrNullCollection){
        // Given
        counters.setUpdatingTrxId(emptyOrNullCollection);

        Mockito.when(userInitiativeCountersRepositoryMock.setUpdatingTrx(counters.getId(), trx.getId()))
                .thenReturn(Mono.just(counters));

        // When
        UserInitiativeCounters result = service.checkUpdatingTrx(trx, counters).block();

        // Then
        checkResult(result);
    }
//endregion

    @Test
    void testWithStuckUpdatingTrx(){
        // Given
        List<String> stuckTrxIds = List.of("TRXID2");
        counters.setUpdatingTrxId(stuckTrxIds);

        Mockito.when(transactionProcessedRepositoryMock.deleteAllById(stuckTrxIds)).thenReturn(Mono.empty());
        Mockito.when(userInitiativeCountersRepositoryMock.setUpdatingTrx(counters.getId(), trx.getId()))
                .thenReturn(Mono.just(counters));
        // When
        UserInitiativeCounters result = service.checkUpdatingTrx(trx, counters).block();

        // Then
        checkResult(result);
    }

    @Test
    void testWithStuckAuthCancellingIt(){
        // Given
        String authTrxId = "TRXID";
        List<String> stuckTrxIds = List.of(authTrxId);
        counters.setUpdatingTrxId(stuckTrxIds);

        trx.setId(authTrxId+ RewardConstants.SYNC_TRX_REFUND_ID_SUFFIX);
        trx.setOperationTypeTranscoded(OperationType.REFUND);

        Mockito.when(transactionProcessedRepositoryMock.deleteAllById(stuckTrxIds)).thenReturn(Mono.empty());
        Mockito.when(userInitiativeCountersRepositoryMock.setUpdatingTrx(counters.getId(), null))
                .thenReturn(Mono.just(counters));

        Mockito.when(userInitiativeCountersRepositoryMock.setUpdatingTrx(counters.getId(), trx.getId()))
                .thenReturn(Mono.error(new IllegalStateException("THIS EXCEPTION SHOULD NOT OCCUR")));
        // When
        Mono<UserInitiativeCounters> mono = service.checkUpdatingTrx(trx, counters);
        ClientExceptionNoBody exception = Assertions.assertThrows(ClientExceptionNoBody.class, mono::block);

        // Then
        Assertions.assertNotNull(exception);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }
}
