package it.gov.pagopa.reward.service.synchronous.op;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.mapper.trx.sync.RewardTransaction2SynchronousTransactionResponseDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.sync.TransactionProcessed2SyncTrxResponseDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class CancelTrxSynchronousServiceTest {

    @Mock
    private TransactionProcessedRepository transactionProcessedRepositoryMock;
    @Mock
    private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock
    private InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeServiceMock;

    private final RewardTransaction2SynchronousTransactionResponseDTOMapper rewardTransaction2SynchronousTransactionResponseDTOMapper = new RewardTransaction2SynchronousTransactionResponseDTOMapper();
    private final TransactionProcessed2SyncTrxResponseDTOMapper transactionProcessed2SyncTrxResponseDTOMapper = new TransactionProcessed2SyncTrxResponseDTOMapper();

    private CancelTrxSynchronousService service;

    @BeforeEach
    void init() {
        service = new CancelTrxSynchronousServiceImpl(
                "01",
                transactionProcessedRepositoryMock,
                userInitiativeCountersRepositoryMock,
                initiativesEvaluatorFacadeServiceMock,
                rewardTransaction2SynchronousTransactionResponseDTOMapper,
                transactionProcessed2SyncTrxResponseDTOMapper);
    }

    @AfterEach
    void verifyNoMoreMockInvocations() {
        Mockito.verifyNoMoreInteractions(
                transactionProcessedRepositoryMock,
                userInitiativeCountersRepositoryMock,
                initiativesEvaluatorFacadeServiceMock
        );
    }

    @Test
    void testNoAuthorized() {
        // Given
        String trxId = "TRXID";
        Mockito.when(transactionProcessedRepositoryMock.findById(trxId)).thenReturn(Mono.empty());

        // When
        SynchronousTransactionResponseDTO result = service.cancelTransaction(trxId).block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void testAlreadyRefunded() {
        // Given
        TransactionProcessed trx = TransactionProcessedFaker.mockInstance(0);
        Mockito.when(transactionProcessedRepositoryMock.findById(trx.getId())).thenReturn(Mono.just(trx));

        TransactionProcessed trxRefund = TransactionProcessedFaker.mockInstance(1);
        Mockito.when(transactionProcessedRepositoryMock.findById(trx.getId() + "_REFUND")).thenReturn(Mono.just(trxRefund));

        // When
        SynchronousTransactionResponseDTO result = service.cancelTransaction(trx.getId()).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(transactionProcessed2SyncTrxResponseDTOMapper.apply(trxRefund, "INITIATIVEID0"), result);
    }

    @Test
    void testNoCounters() {
        test(false);
    }
    @Test
    void testWithCounters() {
        test(true);
    }
    void test(boolean expectCounter) {
        // Given
        TransactionProcessed trx = TransactionProcessedFaker.mockInstance(0);
        String trxRefundId = trx.getId() + "_REFUND";

        Mockito.when(transactionProcessedRepositoryMock.findById(trx.getId())).thenReturn(Mono.just(trx));

        Mockito.when(transactionProcessedRepositoryMock.findById(trxRefundId)).thenReturn(Mono.empty());

        UserInitiativeCounters expectedCounter = new UserInitiativeCounters("USERID0", "INITIATIVEID0");
        Mono<UserInitiativeCounters> expectedCounterMono;
        if(expectCounter){
            expectedCounterMono = Mono.just(expectedCounter);
        } else {
            expectedCounterMono = Mono.empty();
        }
        Mockito.when(userInitiativeCountersRepositoryMock.findByIdThrottled("USERID0_INITIATIVEID0")).thenReturn(expectedCounterMono);

        RewardTransactionDTO expectedReward = RewardTransactionDTOFaker.mockInstance(0);
        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluateAndUpdateBudget(
                Mockito.argThat(t -> assertRefundTrx(t, trx, trxRefundId)
                ),
                Mockito.eq(List.of("INITIATIVEID0")),
                Mockito.argThat(c -> {
                    expectedCounter.setUpdateDate(c.getInitiatives().get("INITIATIVEID0").getUpdateDate());
                    return c.equals(new UserInitiativeCountersWrapper("USERID0", Map.of("INITIATIVEID0", expectedCounter))) &&
                            (!expectCounter || c.getInitiatives().get("INITIATIVEID0") == expectedCounter);
                })
        )).thenReturn(Mono.just(expectedReward));

        // When
        SynchronousTransactionResponseDTO result = service.cancelTransaction(trx.getId()).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rewardTransaction2SynchronousTransactionResponseDTOMapper.apply(trxRefundId, "INITIATIVEID0", expectedReward), result);
    }

    public static boolean assertRefundTrx(TransactionDTO t, TransactionProcessed trx, String trxRefundId) {
        Assertions.assertEquals(trxRefundId, t.getId());
        Assertions.assertTrue(t.getTrxDate().toLocalDateTime().isAfter(trx.getTrxDate()));
        Assertions.assertEquals("01", t.getOperationType());
        Assertions.assertEquals(OperationType.REFUND, t.getOperationTypeTranscoded());
        Assertions.assertEquals(trx.getAmount(), t.getAmount());
        Assertions.assertEquals(trx.getAmountCents(), t.getAmountCents());
        Assertions.assertEquals(TestUtils.bigDecimalValue(0), t.getEffectiveAmount());
        Assertions.assertEquals(new RefundInfo(List.of(trx), Map.of("INITIATIVEID0", new RefundInfo.PreviousReward("INITIATIVEID0", "ORGANIZATION_INITIATIVEID0", trx.getRewards().get("INITIATIVEID0").getAccruedReward()))), t.getRefundInfo());

        Assertions.assertEquals(trx.getIdTrxAcquirer(), t.getIdTrxAcquirer());
        Assertions.assertEquals(trx.getAcquirerCode(), t.getAcquirerCode());
        Assertions.assertEquals(trx.getCorrelationId(), t.getCorrelationId());
        Assertions.assertEquals(trx.getAcquirerId(), t.getAcquirerId());
        Assertions.assertEquals(trx.getRejectionReasons(), t.getRejectionReasons());
        Assertions.assertEquals(trx.getTrxChargeDate(), t.getTrxChargeDate().toLocalDateTime());
        Assertions.assertEquals(trx.getUserId(), t.getUserId());
        Assertions.assertEquals(trx.getChannel(), t.getChannel());

        Assertions.assertNull(t.getHpan());
        Assertions.assertNull(t.getCircuitType());
        Assertions.assertNull(t.getIdTrxIssuer());
        Assertions.assertNull(t.getAmountCurrency());
        Assertions.assertNull(t.getMcc());
        Assertions.assertNull(t.getMerchantId());
        Assertions.assertNull(t.getTerminalId());
        Assertions.assertNull(t.getBin());
        Assertions.assertNull(t.getSenderCode());
        Assertions.assertNull(t.getFiscalCode());
        Assertions.assertNull(t.getVat());
        Assertions.assertNull(t.getPosType());
        Assertions.assertNull(t.getPar());
        Assertions.assertNull(t.getBrandLogo());
        Assertions.assertNull(t.getBrand());
        Assertions.assertNull(t.getMaskedPan());

        return true;
    }
}
