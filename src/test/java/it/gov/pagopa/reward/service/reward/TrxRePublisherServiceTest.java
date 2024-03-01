package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class TrxRePublisherServiceTest {
    @Mock private StreamBridge streamBridgeMock;

    private TrxRePublisherService service;

    @BeforeEach
    void init(){
        service = new TrxRePublisherServiceImpl(streamBridgeMock);
    }

    @Test
    void test(){
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);
        trx.setEffectiveAmount(trx.getAmount().negate());
        trx.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH));
        trx.setRefundInfo(new RefundInfo());

        // When
        service.notify(trx);

        // Then
        assertClearedBody(trx);
        Mockito.verify(streamBridgeMock).send(Mockito.eq("trxResubmitter-out-0"), Mockito.<Message<TransactionDTO>>argThat(m ->
                trx.getUserId().equals(m.getHeaders().get(KafkaHeaders.KEY)) &&
                m.getPayload().equals(trx)
                ));
    }

    private static void assertClearedBody(TransactionDTO republishedTrx) {
        Assertions.assertEquals(Collections.emptyList(), republishedTrx.getRejectionReasons());
        Assertions.assertNull(republishedTrx.getEffectiveAmount());
        Assertions.assertNull(republishedTrx.getRefundInfo());
    }

    @Test
    void trxRePublisherProducerConfigNotNull(){
        TrxRePublisherServiceImpl.TrxRePublisherProducerConfig trxRePublisherProducerConfig = new TrxRePublisherServiceImpl.TrxRePublisherProducerConfig();

        Supplier<Flux<Message<TransactionDTO>>> result = trxRePublisherProducerConfig.trxResubmitter();

        Assertions.assertNotNull(result);

    }
}
