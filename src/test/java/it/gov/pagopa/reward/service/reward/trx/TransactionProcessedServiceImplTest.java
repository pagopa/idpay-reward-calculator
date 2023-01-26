package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.service.reward.TrxRePublisherService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class TransactionProcessedServiceImplTest {

    @Mock
    private OperationTypeHandlerService operationTypeHandlerServiceMock;
    @Mock
    private TransactionProcessedRepository transactionProcessedRepositoryMock;
    @Mock
    private TrxRePublisherService trxRePublisherServiceMock;

    private TransactionProcessedService service;

    private final Transaction2RewardTransactionMapper transaction2RewardTransactionMapper = new Transaction2RewardTransactionMapper();
    private final Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper = new Transaction2TransactionProcessedMapper();

    @BeforeEach
    void init() {
        service = new TransactionProcessedServiceImpl(operationTypeHandlerServiceMock, transaction2TransactionProcessedMapper, transactionProcessedRepositoryMock, trxRePublisherServiceMock);

        Mockito.lenient().when(operationTypeHandlerServiceMock.isChargeOperation(Mockito.any())).thenAnswer(i -> i.getArgument(0, TransactionDTO.class).getOperationType().equals("00"));
    }

    @AfterEach
    void checkMock() {
        Mockito.verifyNoMoreInteractions(operationTypeHandlerServiceMock, transactionProcessedRepositoryMock, trxRePublisherServiceMock);
    }

    // region checkDuplicateTransactions based on findById search
    @Test
    void checkDuplicateTransactions_ChargeNoCorrelationId_Ok() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setCorrelationId("");

        checkDuplicateTransaction_findByIdBased_Ok(trx);
    }

    @Test
    void checkDuplicateTransactions_Refund_Ok() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setOperationType("01");

        checkDuplicateTransaction_findByIdBased_Ok(trx);
    }

    private void checkDuplicateTransaction_findByIdBased_Ok(TransactionDTO trx) {
        Mockito.when(transactionProcessedRepositoryMock.findById(trx.getId())).thenReturn(Mono.empty());

        // When
        TransactionDTO result = service.checkDuplicateTransactions(trx).block();

        // Then
        Assertions.assertSame(trx, result);
        Mockito.verify(operationTypeHandlerServiceMock).isChargeOperation(Mockito.any());
    }

    @Test
    void checkDuplicateTransactionsKo_ChargeNoCorrelationId_Ko() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setCorrelationId("");

        checkDuplicateTransaction_findByIdBased_Ko(trx);
    }

    @Test
    void checkDuplicateTransactionsKo_Refund_Ko() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setOperationType("01");

        checkDuplicateTransaction_findByIdBased_Ko(trx);
    }

    private void checkDuplicateTransaction_findByIdBased_Ko(TransactionDTO trx) {
        TransactionProcessed trxDuplicate = TransactionProcessedFaker.mockInstance(1);
        Mockito.when(transactionProcessedRepositoryMock.findById(trx.getId())).thenReturn(Mono.just(trxDuplicate));

        // When
        TransactionDTO result = service.checkDuplicateTransactions(trx).block();

        // Then
        Assertions.assertNull(result);
        Mockito.verify(operationTypeHandlerServiceMock).isChargeOperation(Mockito.any());
    }

    @Test
    void checkDuplicateTransactions_RefundRecovered_Ok() {
        // Given
        TransactionDTO refund = TransactionDTOFaker.mockInstance(1);
        refund.setOperationType("01");

        RewardTransactionDTO previousRejected = buildDiscardedCorrelatedRefund(refund, 3);
        previousRejected.setId(refund.getId());

        Mockito.when(transactionProcessedRepositoryMock
                        .findById(refund.getId()))
                .thenReturn(Mono.just(previousRejected));

        // When
        TransactionDTO result = service.checkDuplicateTransactions(refund).block();

        // Then
        Assertions.assertSame(refund, result);
        Mockito.verify(operationTypeHandlerServiceMock).isChargeOperation(Mockito.any());
    }
// endregion

    // region checkDuplicateTransactions based on findByAcquiredIdAndCorrelationId
    @Test
    void checkDuplicateTransactions_Charge_Ok() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);

        Mockito.when(transactionProcessedRepositoryMock.findByAcquirerIdAndCorrelationId(trx.getAcquirerId(), trx.getCorrelationId())).thenReturn(Flux.empty());

        // When
        TransactionDTO result = service.checkDuplicateTransactions(trx).block();

        // Then
        Assertions.assertSame(trx, result);
        Mockito.verify(operationTypeHandlerServiceMock).isChargeOperation(Mockito.any());
    }

    @Test
    void checkDuplicateTransactions_ChargeWithPreviousRefunds_Ok() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);

        RewardTransactionDTO correlatedRefund1 = buildDiscardedCorrelatedRefund(trx, 2);
        RewardTransactionDTO correlatedRefund2 = buildDiscardedCorrelatedRefund(trx, 3);

        Mockito.when(transactionProcessedRepositoryMock
                        .findByAcquirerIdAndCorrelationId(trx.getAcquirerId(), trx.getCorrelationId()))
                .thenReturn(Flux.just(correlatedRefund1, correlatedRefund2));

        Mockito.when(trxRePublisherServiceMock.notify(Mockito.any())).thenReturn(true);

        // When
        TransactionDTO result = service.checkDuplicateTransactions(trx).block();

        // Then
        Assertions.assertSame(trx, result);

        Mockito.verify(trxRePublisherServiceMock).notify(Mockito.same(correlatedRefund1));
        Mockito.verify(trxRePublisherServiceMock).notify(Mockito.same(correlatedRefund2));
        Mockito.verify(operationTypeHandlerServiceMock).isChargeOperation(Mockito.any());
    }

    @Test
    void checkDuplicateTransactions_ChargeWithPreviousRefunds_ErrorWhenRePublishing() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);

        RewardTransactionDTO correlatedRefund1 = buildDiscardedCorrelatedRefund(trx, 2);
        RewardTransactionDTO correlatedRefund2 = buildDiscardedCorrelatedRefund(trx, 3);

        Mockito.when(transactionProcessedRepositoryMock
                        .findByAcquirerIdAndCorrelationId(trx.getAcquirerId(), trx.getCorrelationId()))
                .thenReturn(Flux.just(correlatedRefund1, correlatedRefund2));

        Mockito.when(trxRePublisherServiceMock.notify(Mockito.same(correlatedRefund1))).thenReturn(false);

        // When
        Mono<TransactionDTO> mono = service.checkDuplicateTransactions(trx);
        try {
            mono.block();

            // Then
            Assertions.fail("Expected exception");
        } catch (IllegalStateException e) {
            Assertions.assertTrue(e.getMessage().startsWith("[REWARD][REFUND_RECOVER] Something gone wrong while recovering previous refund; trxId IDTRXACQUIRER1ACQUIRERCODE12"), "Unexpected exception message: %s".formatted(e.getMessage()));
        }

        Mockito.verify(trxRePublisherServiceMock).notify(Mockito.same(correlatedRefund1));
        Mockito.verify(trxRePublisherServiceMock, Mockito.never()).notify(Mockito.same(correlatedRefund2));
        Mockito.verify(operationTypeHandlerServiceMock).isChargeOperation(Mockito.any());
    }

    private RewardTransactionDTO buildDiscardedCorrelatedRefund(TransactionDTO trx, int bias) {
        TransactionDTO refund = TransactionDTOFaker.mockInstance(bias);
        refund.setOperationType("01");
        refund.setOperationTypeTranscoded(OperationType.REFUND);
        refund.setAcquirerId(trx.getAcquirerId());
        refund.setCorrelationId(trx.getCorrelationId());
        refund.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH));
        refund.setEffectiveAmount(trx.getAmount().negate());
        return transaction2RewardTransactionMapper.apply(refund);
    }

    @Test
    void checkDuplicateTransactionsKo_Charge_KoDuplicate() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);

        TransactionProcessed trxDuplicate = TransactionProcessedFaker.mockInstance(1);
        trxDuplicate.setId(trx.getId());
        trxDuplicate.setAcquirerId(trx.getAcquirerId());
        trxDuplicate.setCorrelationId(trx.getCorrelationId());
        Mockito.when(transactionProcessedRepositoryMock.findByAcquirerIdAndCorrelationId(trx.getAcquirerId(), trx.getCorrelationId())).thenReturn(Flux.just(trxDuplicate));

        // When
        TransactionDTO result = service.checkDuplicateTransactions(trx).block();

        // Then
        Assertions.assertNull(result);
        Mockito.verify(operationTypeHandlerServiceMock).isChargeOperation(Mockito.any());
    }

    @Test
    void checkDuplicateTransactionsKo_Charge_KoDuplicateCorrelationId() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);

        TransactionProcessed trxDuplicate = TransactionProcessedFaker.mockInstance(1);
        trxDuplicate.setId("OTHERID");
        trxDuplicate.setOperationType("00");
        trxDuplicate.setAcquirerId(trx.getAcquirerId());
        trxDuplicate.setCorrelationId(trx.getCorrelationId());
        Mockito.when(transactionProcessedRepositoryMock.findByAcquirerIdAndCorrelationId(trx.getAcquirerId(), trx.getCorrelationId())).thenReturn(Flux.just(trxDuplicate));

        // When
        TransactionDTO result = service.checkDuplicateTransactions(trx).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID), trx.getRejectionReasons());
        Mockito.verify(operationTypeHandlerServiceMock).isChargeOperation(Mockito.any());
    }
// endregion

    // region save method
    @Test
    void saveCharge() {
        RewardTransactionDTO trx = transaction2RewardTransactionMapper.apply(TransactionDTOFaker.mockInstance(1));

        saveNoRefundDiscarded(trx);
    }

    @Test
    void saveRefundNotDiscarded() {
        RewardTransactionDTO trx = transaction2RewardTransactionMapper.apply(TransactionDTOFaker.mockInstance(1));
        trx.setOperationTypeTranscoded(OperationType.REFUND);

        saveNoRefundDiscarded(trx);
    }

    private void saveNoRefundDiscarded(RewardTransactionDTO trx) {
        TransactionProcessed expectedStored = transaction2TransactionProcessedMapper.apply(trx);
        Mockito.when(transactionProcessedRepositoryMock.save(Mockito.argThat(storingTrx -> {
                    Assertions.assertNotNull(storingTrx.getElaborationDateTime());
                    storingTrx.setElaborationDateTime(null);

                    Assertions.assertEquals(expectedStored, storingTrx);
                    return true;
                })))
                .thenReturn(Mono.just(expectedStored));

        // When
        BaseTransactionProcessed result = service.save(trx).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof TransactionProcessed);
    }

    @Test
    void saveRefundDiscarded() {
        // Given
        RewardTransactionDTO trx = transaction2RewardTransactionMapper.apply(TransactionDTOFaker.mockInstance(1));
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH);

        saveNotElaboratedTransaction(trx);
    }

    @Test
    void saveCorrelationDuplicatedDiscarded() {
        // Given
        RewardTransactionDTO trx = transaction2RewardTransactionMapper.apply(TransactionDTOFaker.mockInstance(1));
        trx.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID);

        saveNotElaboratedTransaction(trx);
    }

    private void saveNotElaboratedTransaction(RewardTransactionDTO trx) {
        Mockito.when(transactionProcessedRepositoryMock.save(Mockito.same(trx))).thenReturn(Mono.just(trx));

        // When
        BaseTransactionProcessed result = service.save(trx).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
    }
// endregion
}