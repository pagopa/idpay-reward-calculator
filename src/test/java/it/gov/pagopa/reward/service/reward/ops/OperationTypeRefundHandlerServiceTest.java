package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        Mockito.when(transactionProcessedRepositoryMock.findAll(
                Example.of(TransactionProcessed.builder()
                        .acquirerId(trx.getAcquirerId())
                        .correlationId(trx.getCorrelationId())
                        .build()))).thenReturn(Flux.empty());

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


        Mockito.when(transactionProcessedRepositoryMock.findAll(
                        Example.of(TransactionProcessed.builder()
                                .acquirerId(trx.getAcquirerId())
                                .correlationId(trx.getCorrelationId())
                                .build())))
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
    void testRefund() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);
        trx.setAmount(BigDecimal.valueOf(2));

        List<TransactionProcessed> expectedPreviousTrxs = List.of(
                TransactionProcessed.builder()
                        .operationTypeTranscoded(OperationType.CHARGE)
                        .trxDate(OffsetDateTime.MIN)
                        .trxChargeDate(OffsetDateTime.MIN)
                        .amount(BigDecimal.TEN).effectiveAmount(BigDecimal.TEN)
                        .rewards(Map.of("INITIATIVE1", new Reward(BigDecimal.ONE), "INITIATIVE2", new Reward(BigDecimal.ONE), "INITIATIVE3", new Reward(BigDecimal.ONE)))
                        .build(),
                TransactionProcessed.builder()
                        .operationTypeTranscoded(OperationType.REFUND)
                        .trxDate(OffsetDateTime.MAX)
                        .trxChargeDate(OffsetDateTime.MIN)
                        .amount(BigDecimal.ONE).effectiveAmount(BigDecimal.valueOf(9))
                        .rewards(Map.of("INITIATIVE1", new Reward(BigDecimal.valueOf(-0.5)), "INITIATIVE3", new Reward(BigDecimal.valueOf(-1))))
                        .build()
        );

        Mockito.when(transactionProcessedRepositoryMock.findAll(Mockito.<Example<TransactionProcessed>>argThat(i ->
                        i.getProbe().getAcquirerId().equals(trx.getAcquirerId()) &&
                                i.getProbe().getCorrelationId().equals(trx.getCorrelationId())
                )))
                .thenReturn(Flux.fromIterable(expectedPreviousTrxs));

        // When
        final TransactionDTO result = operationTypeRefundHandlerService.handleRefundOperation(trx).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(OperationType.REFUND, result.getOperationTypeTranscoded());
        Assertions.assertEquals(OffsetDateTime.MIN, result.getTrxChargeDate());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(7), result.getEffectiveAmount());
        Assertions.assertEquals(Collections.emptyList(), result.getRejectionReasons());
        Assertions.assertNotNull(result.getRefundInfo());

        Assertions.assertEquals(expectedPreviousTrxs, result.getRefundInfo().getPreviousTrxs());
        Assertions.assertEquals(
                Map.of("INITIATIVE1", scaleBigDecimal(BigDecimal.valueOf(0.5)), "INITIATIVE2", scaleBigDecimal(BigDecimal.ONE))
                , result.getRefundInfo().getPreviousRewards());
    }

    private BigDecimal scaleBigDecimal(BigDecimal bigDecimal) {
        return bigDecimal.setScale(2, RoundingMode.UNNECESSARY);
    }
}
