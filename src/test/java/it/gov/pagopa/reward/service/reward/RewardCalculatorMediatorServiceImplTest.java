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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class RewardCalculatorMediatorServiceImplTest {

    @Test
    void execute() {
        // Given
        TransactionFilterService transactionFilterService = Mockito.mock(TransactionFilterServiceImpl.class);
        OnboardedInitiativesService onboardedInitiativesService = Mockito.mock(OnboardedInitiativesServiceImpl.class);
        UserInitiativeCountersRepository userInitiativeCountersRepository = Mockito.mock(UserInitiativeCountersRepository.class);
        RuleEngineService ruleEngineService = Mockito.mock(RuleEngineServiceImpl.class);
        UserInitiativeCountersUpdateService userInitiativeCountersUpdateService = Mockito.mock(UserInitiativeCountersUpdateService.class);
        RewardCalculatorMediatorService rewardCalculatorMediatorService = new RewardCalculatorMediatorServiceImpl(transactionFilterService, onboardedInitiativesService, userInitiativeCountersRepository, ruleEngineService, userInitiativeCountersUpdateService);

        TransactionDTO trx1 = Mockito.mock(TransactionDTO.class);
        TransactionDTO trx2 = Mockito.mock(TransactionDTO.class);
        Flux<TransactionDTO> trxFlux = Flux.just(trx1, trx2);

        Mockito.when(transactionFilterService.filter(Mockito.same(trx1))).thenReturn(true);
        Mockito.when(transactionFilterService.filter(Mockito.same(trx2))).thenReturn(false);

        UserInitiativeCounters uCounters1 = UserInitiativeCounters.builder()
                .userId("USER1")
                .initiatives(
                        new HashMap<>(Map.of(
                                "INITIATIVE1",
                                InitiativeCounters.builder().exhaustedBudget(false).build()
                        ))
                )
                .build();
        Mockito.when(userInitiativeCountersRepository.findById(Mockito.<String>any())).thenReturn(Mono.just(uCounters1));
        Mockito.when(userInitiativeCountersRepository.save(Mockito.any())).thenReturn(Mono.empty());

        List<String> initiatives1 = List.of("INITIATIVE1");
        Mockito.when(onboardedInitiativesService.getInitiatives(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromIterable(initiatives1));

        RewardTransactionDTO rTrx1 = Mockito.mock(RewardTransactionDTO.class);
        Mockito.when(ruleEngineService.applyRules(Mockito.same(trx1), Mockito.eq(initiatives1), Mockito.any()))
                .thenReturn(rTrx1);

        // When
        Flux<RewardTransactionDTO> result = rewardCalculatorMediatorService.execute(trxFlux);

        // Then
        Assertions.assertEquals(1L, result.count().block());

        Mockito.verify(userInitiativeCountersRepository).save(Mockito.any());
    }

    @Test
    void testExecuteWithExhaustedInitiatives() {
        // Given
        TransactionFilterService transactionFilterService = Mockito.mock(TransactionFilterServiceImpl.class);
        OnboardedInitiativesService onboardedInitiativesService = Mockito.mock(OnboardedInitiativesServiceImpl.class);
        UserInitiativeCountersRepository userInitiativeCountersRepository = Mockito.mock(UserInitiativeCountersRepository.class);
        RuleEngineService ruleEngineService = Mockito.mock(RuleEngineServiceImpl.class);
        UserInitiativeCountersUpdateService userInitiativeCountersUpdateService = Mockito.mock(UserInitiativeCountersUpdateService.class);
        RewardCalculatorMediatorService rewardCalculatorMediatorService = new RewardCalculatorMediatorServiceImpl(transactionFilterService, onboardedInitiativesService, userInitiativeCountersRepository, ruleEngineService, userInitiativeCountersUpdateService);

        TransactionDTO trx1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO trx2 = Mockito.mock(TransactionDTO.class);
        Flux<TransactionDTO> trxFlux = Flux.just(trx1, trx2);

        Mockito.when(transactionFilterService.filter(Mockito.same(trx1))).thenReturn(true);
        Mockito.when(transactionFilterService.filter(Mockito.same(trx2))).thenReturn(false);

        UserInitiativeCounters uCounters1 = UserInitiativeCounters.builder()
                .userId("USER1")
                .initiatives(
                        new HashMap<>(Map.of(
                                "INITIATIVE1",
                                InitiativeCounters.builder().initiativeId("INITIATIVE1").exhaustedBudget(true).build(),
                                "INITIATIVE2",
                                InitiativeCounters.builder().initiativeId("INITIATIVE2").exhaustedBudget(false).build()
                        ))
                )
                .build();
        Mockito.when(userInitiativeCountersRepository.findById(Mockito.<String>any())).thenReturn(Mono.just(uCounters1));
        Mockito.when(userInitiativeCountersRepository.save(Mockito.any())).thenReturn(Mono.empty());

        List<String> initiatives1 = List.of("INITIATIVE1", "INITIATIVE2");
        Mockito.when(onboardedInitiativesService.getInitiatives(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromIterable(initiatives1));

        RewardTransactionDTO rTrx1 = RewardTransactionDTO.builder()
                .rewards(
                        new HashMap<>(Map.of("INITIATIVE2", new Reward()))
                ).build();
        Mockito.when(ruleEngineService.applyRules(trx1, List.of("INITIATIVE2"), uCounters1))
                .thenReturn(rTrx1);

        // When
        Flux<RewardTransactionDTO> result = rewardCalculatorMediatorService.execute(trxFlux);

        // Then
        Assertions.assertEquals(1L, result.count().block());
        result.subscribe(r -> {
            Assertions.assertEquals(List.of("BUDGET_EXHAUSTED_for_initiativeId_INITIATIVE1"), r.getRejectionReasons());
        });

        Mockito.verify(userInitiativeCountersRepository).save(Mockito.any());
    }
}