package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class OperationTypeRefundHandlerServiceTest {

    @Mock
    private TransactionProcessedRepository transactionProcessedRepositoryMock;

    private OperationTypeRefundHandlerService operationTypeRefundHandlerService;

    @BeforeEach
    void init() {
        operationTypeRefundHandlerService = new OperationTypeRefundHandlerServiceImpl(transactionProcessedRepositoryMock);
    }

    @Test
    void testRefundInvalid() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);
        trx.setCorrelationId("");

        // When
        final TransactionDTO result = operationTypeRefundHandlerService.handleRefundOperation(trx).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(OperationType.REFUND, result.getOperationTypeTranscoded());
        Assertions.assertNull(result.getTrxChargeDate());
        Assertions.assertNull(result.getEffectiveAmount());
        Assertions.assertNull(result.getRefundInfo());
        Assertions.assertEquals(List.of("INVALID_REFUND"), result.getRejectionReasons());
    }

    @Test
    void testRefundNotMatch() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);

        Mockito.when(transactionProcessedRepositoryMock.findByAcquirerIdAndCorrelationIdRetryable(trx.getAcquirerId(), trx.getCorrelationId()))
                        .thenReturn(Flux.empty());

        // When
        final TransactionDTO result = operationTypeRefundHandlerService.handleRefundOperation(trx).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(OperationType.REFUND, result.getOperationTypeTranscoded());
        Assertions.assertNull(result.getTrxChargeDate());
        Assertions.assertNull(result.getEffectiveAmount());
        Assertions.assertNull(result.getRefundInfo());
        Assertions.assertEquals(List.of("REFUND_NOT_MATCH"), result.getRejectionReasons());
    }

    @Test
    void testRefundNoMatchCharge() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);


        Mockito.when(transactionProcessedRepositoryMock.findByAcquirerIdAndCorrelationIdRetryable(trx.getAcquirerId(), trx.getCorrelationId()))
                .thenReturn(Flux.just(TransactionProcessed.builder().operationTypeTranscoded(OperationType.REFUND).amount(BigDecimal.ONE).rewards(Collections.emptyMap()).build()));

        // When
        final TransactionDTO result = operationTypeRefundHandlerService.handleRefundOperation(trx).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(OperationType.REFUND, result.getOperationTypeTranscoded());
        Assertions.assertNull(result.getTrxChargeDate());
        Assertions.assertNull(result.getEffectiveAmount());
        Assertions.assertNull(result.getRefundInfo());
        Assertions.assertEquals(List.of("REFUND_NOT_MATCH"), result.getRejectionReasons());
    }

    @Test
    void testRefund_RewardTransactionDto(){
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);
        trx.setAmount(BigDecimal.valueOf(2));

        List<BaseTransactionProcessed> expectedPreviousTrxs = List.of(
                TransactionProcessed.builder()
                        .operationTypeTranscoded(OperationType.CHARGE)
                        .trxDate(LocalDateTime.MIN)
                        .trxChargeDate(LocalDateTime.MIN)
                        .amount(BigDecimal.TEN).effectiveAmount(BigDecimal.TEN)
                        .rewards(Map.of(
                                "INITIATIVE1", new Reward("INITIATIVE1", "ORGANIZATION", BigDecimal.ONE),
                                "INITIATIVE2", new Reward("INITIATIVE2", "ORGANIZATION", BigDecimal.ONE),
                                "INITIATIVE3", new Reward("INITIATIVE3", "ORGANIZATION", BigDecimal.ONE)))
                        .build(),

                // a past refund discarded
                RewardTransactionDTO.builder()
                        .operationTypeTranscoded(OperationType.REFUND)
                        .rejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH))
                        .trxDate(OffsetDateTime.MIN)
                        .trxChargeDate(OffsetDateTime.MIN)
                        .amount(BigDecimal.ONE).effectiveAmount(BigDecimal.ONE)
                        .build(),

                // a past refund elaborated
                TransactionProcessed.builder()
                        .operationTypeTranscoded(OperationType.REFUND)
                        .trxDate(LocalDateTime.MAX)
                        .trxChargeDate(LocalDateTime.MIN)
                        .amount(BigDecimal.ONE).effectiveAmount(BigDecimal.valueOf(9))
                        .rewards(Map.of(
                                "INITIATIVE1", new Reward("INITIATIVE1","ORGANIZATION", BigDecimal.valueOf(-0.5)),
                                "INITIATIVE3", new Reward("INITIATIVE3","ORGANIZATION", BigDecimal.valueOf(-1))))
                        .build()
        );

        Mockito.when(transactionProcessedRepositoryMock.findByAcquirerIdAndCorrelationIdRetryable(trx.getAcquirerId(), trx.getCorrelationId()))
                .thenReturn(Flux.fromIterable(expectedPreviousTrxs));

        // When
        final TransactionDTO result = operationTypeRefundHandlerService.handleRefundOperation(trx).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(OperationType.REFUND, result.getOperationTypeTranscoded());
        Assertions.assertEquals(OffsetDateTime.MIN.toLocalDateTime(), result.getTrxChargeDate().toLocalDateTime());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(7), result.getEffectiveAmount());
        Assertions.assertEquals(Collections.emptyList(), result.getRejectionReasons());
        Assertions.assertNotNull(result.getRefundInfo());

        Assertions.assertEquals(List.of(expectedPreviousTrxs.get(0), expectedPreviousTrxs.get(2)), result.getRefundInfo().getPreviousTrxs());
        Assertions.assertEquals(
                Map.of(
                        "INITIATIVE1", new RefundInfo.PreviousReward("INITIATIVE1", "ORGANIZATION", scaleBigDecimal(BigDecimal.valueOf(0.5))),
                        "INITIATIVE3", new RefundInfo.PreviousReward("INITIATIVE3", "ORGANIZATION", scaleBigDecimal(BigDecimal.ZERO)),
                        "INITIATIVE2", new RefundInfo.PreviousReward("INITIATIVE2", "ORGANIZATION", scaleBigDecimal(BigDecimal.ONE)))
                , result.getRefundInfo().getPreviousRewards());
    }

    private BigDecimal scaleBigDecimal(BigDecimal bigDecimal) {
        return bigDecimal.setScale(2, RoundingMode.UNNECESSARY);
    }
}
