package it.gov.pagopa.event.processor;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.RewardsTransactionDTO;
import it.gov.pagopa.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
@Slf4j
class TransactionProcessorTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionProcessor transactionProcessor;



    @Test
    void testTrxProcessorStepByStep() {
        TransactionDTO ingestedTrx = TransactionDTO.builder()
                .idTrxAcquirer("98174002165501220007165503")
                .acquirerCode("36081")
                .trxDate("2020-09-07T15:58:42.000+00:00")
                .hpan("5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0")
                .operationType("00")
                .amount(new BigDecimal("200.00"))
                .acquirerId("09509")
                .build();

        Map<String, BigDecimal> rewards = new HashMap<>();
        rewards.put("initiative",new BigDecimal("60.00"));

        Mockito.when(transactionService.applyRules(any(TransactionDTO.class))).thenReturn(new RewardsTransactionDTO(ingestedTrx,rewards));

        Flux<RewardsTransactionDTO> test = transactionProcessor.trxProcessor().apply(Flux.just(ingestedTrx));
        StepVerifier.create(test)
                .expectNextMatches(trx -> trx.getRewards().get("initiative").equals(new BigDecimal("60.00")))
                .expectComplete()
                .verify();
    }
}






