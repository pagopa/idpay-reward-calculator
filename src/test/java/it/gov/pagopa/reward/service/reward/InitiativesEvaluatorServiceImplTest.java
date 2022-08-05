package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InitiativesEvaluatorServiceImplTest {

    @Test
    void evaluateInitiativesRuleAndBudget() {

        // Given
        RuleEngineService ruleEngineService = Mockito.mock(RuleEngineServiceImpl.class);

        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        List<String> initiatives = List.of("INITIATIVE1", "INITIATIVE2");

        UserInitiativeCounters userCounters = UserInitiativeCounters.builder()
                .userId("USER1")
                .initiatives(Map.of(
                                "INITIATIVE1",
                                InitiativeCounters.builder().initiativeId("INITIATIVE1").exhaustedBudget(true).build(),
                                "INITIATIVE2",
                                InitiativeCounters.builder().initiativeId("INITIATIVE2").exhaustedBudget(false).build()
                        )
                )
                .build();

        Reward reward = new Reward(new BigDecimal("200"));
        RewardTransactionDTO rTrx = RewardTransactionDTO.builder()
                .rewards(Map.of("INITIATIVE2", reward)).build();
        Mockito.when(ruleEngineService.applyRules(trx, List.of("INITIATIVE2"), userCounters)).thenReturn(rTrx);

        InitiativesEvaluatorService initiativesEvaluatorService = new InitiativesEvaluatorServiceImpl(ruleEngineService);

        // When
        RewardTransactionDTO result = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Map.of("INITIATIVE1", List.of("BUDGET_EXHAUSTED")), result.getInitiativeRejectionReasons());
    }
}