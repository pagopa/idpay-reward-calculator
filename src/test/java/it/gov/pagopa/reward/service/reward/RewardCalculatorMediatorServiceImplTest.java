package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class RewardCalculatorMediatorServiceImplTest {

    @Test
    void execute() {
        // Given
        OnboardedInitiativesService onboardedInitiativesService = Mockito.mock(OnboardedInitiativesServiceImpl.class);
        UserInitiativeCountersRepository userInitiativeCountersRepository = Mockito.mock(UserInitiativeCountersRepository.class);
        UserInitiativeCountersUpdateService userInitiativeCountersUpdateService = Mockito.mock(UserInitiativeCountersUpdateService.class);
        InitiativesEvaluatorService initiativesEvaluatorService = Mockito.mock(InitiativesEvaluatorServiceImpl.class);
        RewardCalculatorMediatorService rewardCalculatorMediatorService = new RewardCalculatorMediatorServiceImpl(onboardedInitiativesService, userInitiativeCountersRepository, initiativesEvaluatorService, userInitiativeCountersUpdateService);

        TransactionDTO trx1 = Mockito.mock(TransactionDTO.class);
        TransactionDTO trx2 = Mockito.mock(TransactionDTO.class);
        Flux<TransactionDTO> trxFlux = Flux.just(trx1, trx2);

        Mockito.when(userInitiativeCountersRepository.findById(Mockito.<String>any())).thenReturn(Mono.empty());
        Mockito.when(userInitiativeCountersRepository.save(Mockito.any())).thenReturn(Mono.empty());

        List<String> initiatives1 = List.of("INITIATIVE1");
        Mockito.when(onboardedInitiativesService.getInitiatives(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromIterable(initiatives1));

        RewardTransactionDTO rTrx1 = Mockito.mock(RewardTransactionDTO.class);
        Mockito.when(initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(Mockito.same(trx1), Mockito.eq(initiatives1), Mockito.any()))
                .thenReturn(rTrx1);

        // When
        Flux<RewardTransactionDTO> result = rewardCalculatorMediatorService.execute(trxFlux);

        // Then
        Assertions.assertEquals(1L, result.count().block());

        Mockito.verify(userInitiativeCountersRepository).save(Mockito.any());
    }
}