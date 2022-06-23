package it.gov.pagopa.service;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.mapper.TransactionMapper;
import it.gov.pagopa.event.processor.TransactionProcessor;
import it.gov.pagopa.model.RewardTransaction;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Slf4j
class TransactionServiceImplTest {

    @Mock
    private StatelessKieSession statelessKieSession;

    @Mock
    private TransactionMapper trxMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private TransactionDTO ingestedTrx;

    private RewardTransaction expected;

    @BeforeEach
    void init(){
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
    void applyRules() {
        Mockito.when(trxMapper.map(any(TransactionDTO.class))).thenReturn(expected);
        RewardTransaction actual = transactionService.applyRules(ingestedTrx);

        assertEquals(expected.getReward(),actual.getReward());
    }
}