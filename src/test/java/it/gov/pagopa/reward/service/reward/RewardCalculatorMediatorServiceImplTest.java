package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class RewardCalculatorMediatorServiceImplTest {

    @Test
    void execute() {
        // Given
        TransactionFilterService transactionFilterService = Mockito.mock(TransactionFilterServiceImpl.class);
        OnboardedInitiativesService onboardedInitiativesService = Mockito.mock(OnboardedInitiativesServiceImpl.class);
        RuleEngineService ruleEngineService = Mockito.mock(RuleEngineServiceImpl.class);
        RewardCalculatorMediatorService rewardCalculatorMediatorService = new RewardCalculatorMediatorServiceImpl(transactionFilterService, onboardedInitiativesService,ruleEngineService);

        TransactionDTO trx1 = Mockito.mock(TransactionDTO.class);
        TransactionDTO trx2 = Mockito.mock(TransactionDTO.class);
        Flux<TransactionDTO> trxFlux = Flux.just(trx1,trx2);

        Mockito.when(transactionFilterService.filter(Mockito.same(trx1))).thenReturn(true);
        Mockito.when(transactionFilterService.filter(Mockito.same(trx2))).thenReturn(false);

        List<String> initiatives1 = Mockito.mock(List.class);
        Mockito.when(onboardedInitiativesService.getInitiatives(Mockito.any(),Mockito.any()))
                .thenReturn(initiatives1);

        RewardTransactionDTO rTrx1 = Mockito.mock(RewardTransactionDTO.class);
        Mockito.when(ruleEngineService.applyRules(Mockito.same(trx1),Mockito.same(initiatives1)))
                .thenReturn(rTrx1);

        // When
        Flux<RewardTransactionDTO> result = rewardCalculatorMediatorService.execute(trxFlux);

        // Then
        result.count().subscribe(i -> assertEquals(1L,i));


    }
}