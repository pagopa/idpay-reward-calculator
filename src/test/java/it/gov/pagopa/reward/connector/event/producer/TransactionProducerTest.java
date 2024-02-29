package it.gov.pagopa.reward.connector.event.producer;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

class TransactionProducerTest {

    @Test
    void trxManyBeanNotNull() {
        TransactionProducer transactionProducer = new TransactionProducer();

        Sinks.Many<TransactionDTO> result = transactionProducer.trxMany();

        Assertions.assertNotNull(result);
    }

}