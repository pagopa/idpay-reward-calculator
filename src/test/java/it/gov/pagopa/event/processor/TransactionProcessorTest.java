package it.gov.pagopa.event.processor;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.mapper.TransactionMapper;
import it.gov.pagopa.model.RewardTransaction;
import it.gov.pagopa.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
@Slf4j
class TransactionProcessorTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionProcessor transactionProcessor;

    private TransactionDTO ingestedTrx;
    private RewardTransaction expected;

    @BeforeEach
    void init() {
        ingestedTrx = TransactionDTO.builder()
                .idTrxAcquirer("98174002165501220007165503")
                .acquirerCode("36081")
                .trxDate("2020-09-07T15:58:42.000+00:00")
                .hpan("5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0")
                .operationType("00")
                .amount(new BigDecimal("200.00"))
                .acquirerId("09509")
                .build();

        expected = RewardTransaction.builder()
                .idTrxAcquirer("98174002165501220007165503")
                .acquirerCode("36081")
                .trxDate("2020-09-07T15:58:42.000+00:00")
                .hpan("5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0")
                .operationType("00")
                .amount(new BigDecimal("200.00"))
                .acquirerId("09509")
                .reward(new BigDecimal("60.00"))
                .build();
    }


    @Test
    void testTrxProcessorStepByStep() {
        Mockito.when(transactionService.applyRules(any(TransactionDTO.class))).thenReturn(expected);


        Flux<TransactionDTO> ingestedFlux = Flux.just(ingestedTrx);
        Flux<RewardTransaction> test = transactionProcessor.trxProcessor().apply(ingestedFlux);

        StepVerifier.create(test)
                .expectNextMatches(trx -> trx.getReward().equals(new BigDecimal("60.00")))
                .expectComplete()
                .verify();
    }
}






