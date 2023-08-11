package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class InitiativesEvaluatorServiceImplTest {

    @Test
    void evaluateInitiativesRuleAndBudget() {

        // Given
        RuleEngineService ruleEngineService = Mockito.mock(RuleEngineServiceImpl.class);

        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        List<String> initiatives = List.of("INITIATIVE1", "INITIATIVE2");

        UserInitiativeCountersWrapper userCounters = UserInitiativeCountersWrapper.builder()
                .entityId("USER1")
                .initiatives(Map.of(
                                "INITIATIVE1",
                                UserInitiativeCounters.builder(trx.getUserId(), InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,"INITIATIVE1").exhaustedBudget(true).build(),
                                "INITIATIVE2",
                                UserInitiativeCounters.builder(trx.getUserId(), InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,"INITIATIVE2").exhaustedBudget(false).build()
                        )
                )
                .build();

        Reward reward = new Reward("INITIATIVE2", "ORGANIZATION", new BigDecimal("200"));
        Mockito.when(ruleEngineService.applyRules(Mockito.eq(trx), Mockito.any(), Mockito.eq(userCounters))).thenAnswer(i->
            RewardTransactionDTO.builder()
                    .rewards(Map.of("INITIATIVE2", reward)).build()
        );

        InitiativesEvaluatorService initiativesEvaluatorService = new InitiativesEvaluatorServiceImpl(ruleEngineService);

        // When
        RewardTransactionDTO result = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);

        // Then
        Assertions.assertNotNull(result);
        final Map<String, List<String>> rejectedInitiatives = Map.of("INITIATIVE1", List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));
        Assertions.assertEquals(rejectedInitiatives, result.getInitiativeRejectionReasons());
        Mockito.verify(ruleEngineService).applyRules(trx, List.of("INITIATIVE2"), userCounters);

        // Given refund having not rewarded charge
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        // When
        result = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rejectedInitiatives, result.getInitiativeRejectionReasons());
        Mockito.verify(ruleEngineService, Mockito.times(2)).applyRules(trx, List.of("INITIATIVE2"), userCounters);

        // Given refund having rewarded charge equal to 0
        trx.setRefundInfo(new RefundInfo());
        trx.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE1", new RefundInfo.PreviousReward("INITIATIVE1", "ORGANIZATION", BigDecimal.ZERO)));
        // When
        result = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rejectedInitiatives, result.getInitiativeRejectionReasons());
        Mockito.verify(ruleEngineService, Mockito.times(3)).applyRules(trx, List.of("INITIATIVE2"), userCounters);

        // Given refund having rewarded charge
        trx.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE1", new RefundInfo.PreviousReward("INITIATIVE1", "ORGANIZATION", BigDecimal.ONE)));

        // When
        result = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Collections.emptyMap(), result.getInitiativeRejectionReasons());
        Mockito.verify(ruleEngineService).applyRules(trx, List.of("INITIATIVE1", "INITIATIVE2"), userCounters);
    }
}