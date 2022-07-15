package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.TransactionMapper;
import it.gov.pagopa.reward.model.RewardTransaction;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Slf4j
class RuleEngineServiceImplTest {

    @Test
    void applyRules() {
        // Given
        DroolsContainerHolderService droolsContainerHolderService = Mockito.mock(DroolsContainerHolderServiceImpl.class);
        TransactionMapper transactionMapper = Mockito.mock(TransactionMapper.class);
        RewardTransactionMapper rewardTransactionMapper = Mockito.mock(RewardTransactionMapper.class);

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(droolsContainerHolderService,transactionMapper,rewardTransactionMapper);

        TransactionDTO trx = Mockito.mock(TransactionDTO.class);
        List<String> initiatives = new InitiativesServiceImpl().getInitiatives(trx.getHpan(), trx.getTrxDate());

        RewardTransaction rewardTrx = Mockito.mock(RewardTransaction.class);
        Mockito.when(transactionMapper.map(Mockito.same(trx))).thenReturn(rewardTrx);

        KieContainer kieContainer = Mockito.mock(KieContainer.class);
        Mockito.when(droolsContainerHolderService.getKieContainer()).thenReturn(kieContainer);
        StatelessKieSession statelessKieSession = Mockito.mock(StatelessKieSession.class);
        Mockito.when(kieContainer.newStatelessKieSession()).thenReturn(statelessKieSession);

        RewardTransactionDTO rewardTrxDto = Mockito.mock(RewardTransactionDTO.class);
        Mockito.when(rewardTransactionMapper.map(Mockito.same(rewardTrx))).thenReturn(rewardTrxDto);

        // When
        ruleEngineService.applyRules(trx, initiatives);

        // Then
        Mockito.verify(transactionMapper).map(Mockito.same(trx));
        Mockito.verify(droolsContainerHolderService).getKieContainer();
        Mockito.verify(statelessKieSession).execute(Mockito.any(Command.class));
        Mockito.verify(rewardTransactionMapper).map(Mockito.same(rewardTrx));
    }

    @Test
    void applyRulesWithEmptyInitiatives() {
        // Given
        DroolsContainerHolderService droolsContainerHolderService = Mockito.mock(DroolsContainerHolderServiceImpl.class);
        TransactionMapper transactionMapper = Mockito.mock(TransactionMapper.class);
        RewardTransactionMapper rewardTransactionMapper = Mockito.mock(RewardTransactionMapper.class);

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(droolsContainerHolderService,transactionMapper,rewardTransactionMapper);

        TransactionDTO trx = Mockito.mock(TransactionDTO.class);
        List<String> initiatives = new ArrayList<>();

        // When
        ruleEngineService.applyRules(trx, initiatives);

        // Then
        Mockito.verify(droolsContainerHolderService,Mockito.never()).getKieContainer();
        Mockito.verify(transactionMapper,Mockito.never()).map(Mockito.same(trx));
        Mockito.verify(rewardTransactionMapper,Mockito.never()).map(Mockito.any());
    }
}