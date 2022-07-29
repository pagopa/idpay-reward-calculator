package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionDroolsMapper;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
class RuleEngineServiceImplTest {

    @Test
    void applyRules() {
        // Given
        RewardContextHolderService rewardContextHolderService = Mockito.mock(RewardContextHolderServiceImpl.class);
        Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper = Mockito.mock(Transaction2TransactionDroolsMapper.class);
        TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper = Mockito.mock(TransactionDroolsDTO2RewardTransactionMapper.class);

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(rewardContextHolderService, transaction2TransactionDroolsMapper, transactionDroolsDTO2RewardTransactionMapper);

        TransactionDTO trx = Mockito.mock(TransactionDTO.class);
        List<String> initiatives =  List.of("Initiative1");
        UserInitiativeCounters counters = new UserInitiativeCounters("userId", new HashMap<>());

        TransactionDroolsDTO rewardTrx = Mockito.mock(TransactionDroolsDTO.class);
        Mockito.when(transaction2TransactionDroolsMapper.apply(Mockito.same(trx))).thenReturn(rewardTrx);

        KieContainer kieContainer = Mockito.mock(KieContainer.class);
        Mockito.when(rewardContextHolderService.getRewardRulesKieContainer()).thenReturn(kieContainer);
        StatelessKieSession statelessKieSession = Mockito.mock(StatelessKieSession.class);
        Mockito.when(kieContainer.newStatelessKieSession()).thenReturn(statelessKieSession);

        RewardTransactionDTO rewardTrxDto = Mockito.mock(RewardTransactionDTO.class);
        Mockito.when(transactionDroolsDTO2RewardTransactionMapper.apply(Mockito.same(rewardTrx))).thenReturn(rewardTrxDto);

        // When
        ruleEngineService.applyRules(trx, initiatives, counters);

        // Then
        Mockito.verify(transaction2TransactionDroolsMapper).apply(Mockito.same(trx));
        Mockito.verify(rewardContextHolderService).getRewardRulesKieContainer();
        Mockito.verify(statelessKieSession).execute(Mockito.any(Command.class));
        Mockito.verify(transactionDroolsDTO2RewardTransactionMapper).apply(Mockito.same(rewardTrx));
    }

    @Test
    void applyRulesWithEmptyInitiatives() {
        // Given
        RewardContextHolderService rewardContextHolderService = Mockito.mock(RewardContextHolderServiceImpl.class);
        Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper = Mockito.mock(Transaction2TransactionDroolsMapper.class);
        TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper = Mockito.mock(TransactionDroolsDTO2RewardTransactionMapper.class);

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(rewardContextHolderService, transaction2TransactionDroolsMapper, transactionDroolsDTO2RewardTransactionMapper);

        TransactionDTO trx = Mockito.mock(TransactionDTO.class);
        List<String> initiatives = new ArrayList<>();
        UserInitiativeCounters counters = new UserInitiativeCounters("userId", new HashMap<>());

        TransactionDroolsDTO rewardTrx = Mockito.mock(TransactionDroolsDTO.class);
        Mockito.when(transaction2TransactionDroolsMapper.apply(Mockito.same(trx))).thenReturn(rewardTrx);

        // When
        ruleEngineService.applyRules(trx, initiatives, counters);

        // Then
        Mockito.verify(rewardContextHolderService,Mockito.never()).getRewardRulesKieContainer();

        Mockito.verify(transaction2TransactionDroolsMapper).apply(Mockito.same(trx));
        Mockito.verify(rewardContextHolderService, Mockito.never()).getRewardRulesKieContainer();
        Mockito.verify(transactionDroolsDTO2RewardTransactionMapper).apply(Mockito.same(rewardTrx));
    }
}