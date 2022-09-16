package it.gov.pagopa.reward.service.reward.evaluate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionDroolsMapper;
import it.gov.pagopa.reward.dto.mapper.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.*;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderServiceImpl;
import it.gov.pagopa.reward.service.reward.evaluate.RuleEngineService;
import it.gov.pagopa.reward.service.reward.evaluate.RuleEngineServiceImpl;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
@Slf4j
class RuleEngineServiceImplTest {

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.drools.compiler.kie.builder.impl")).setLevel(Level.WARN);
    }

    @Test
    void applyRules() {
        // Given
        RewardContextHolderService rewardContextHolderService = Mockito.mock(RewardContextHolderServiceImpl.class);
        Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper = Mockito.mock(Transaction2TransactionDroolsMapper.class);
        TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper = Mockito.mock(TransactionDroolsDTO2RewardTransactionMapper.class);

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(new RuleEngineConfig(), rewardContextHolderService, transaction2TransactionDroolsMapper, transactionDroolsDTO2RewardTransactionMapper);

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

        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(new RuleEngineConfig(), rewardContextHolderService, transaction2TransactionDroolsMapper, transactionDroolsDTO2RewardTransactionMapper);

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

    @Test
    void testComplete(){
        testComplete(false);
    }
    @Test
    void testCompleteShortCircuited(){
        testComplete(true);
    }
    void testComplete(boolean shortCircuited){
        // given
        RewardRule2DroolsRuleService droolsRuleService = RewardRule2DroolsRuleServiceTest.buildRewardRule2DroolsRule(false);
        KieContainerBuilderService kieContainerBuilder = new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class));

        List<DroolsRule> rules = List.of(
                droolsRuleService.apply(InitiativeReward2BuildDTOFaker.mockInstance(0, Set.of(ThresholdDTO.class, MccFilterDTO.class), null)),
                droolsRuleService.apply(InitiativeReward2BuildDTOFaker.mockInstance(1, Set.of(ThresholdDTO.class, MccFilterDTO.class), null)),
                droolsRuleService.apply(InitiativeReward2BuildDTOFaker.mockInstance(2, Set.of(ThresholdDTO.class), null))
        );

        RewardContextHolderService rewardContextHolderServiceMock = Mockito.mock(RewardContextHolderServiceImpl.class);
        Mockito.when(rewardContextHolderServiceMock.getRewardRulesKieContainer()).thenReturn(kieContainerBuilder.build(Flux.fromIterable(rules)).block());

        RuleEngineConfig ruleEngineConfig = new RuleEngineConfig();
        ruleEngineConfig.setShortCircuitConditions(shortCircuited);
        RuleEngineService ruleEngineService = new RuleEngineServiceImpl(ruleEngineConfig, rewardContextHolderServiceMock, new Transaction2TransactionDroolsMapper(), new TransactionDroolsDTO2RewardTransactionMapper());

        // when
        TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(1)
                .amount(BigDecimal.valueOf(11))
                .mcc("MCC_0")
                .build();
        RewardTransactionDTO result = ruleEngineService.applyRules(trx, rules.stream().map(DroolsRule::getId).collect(Collectors.toList()), new UserInitiativeCounters(trx.getUserId(), new HashMap<>()));

        Assertions.assertEquals(Map.of(
                "ID_0_ssx", shortCircuited ? List.of("TRX_RULE_MCCFILTER_FAIL") :List.of("TRX_RULE_MCCFILTER_FAIL","TRX_RULE_THRESHOLD_FAIL"),
                "ID_1_rah", List.of("TRX_RULE_THRESHOLD_FAIL")
        ), result.getInitiativeRejectionReasons());

        Assertions.assertEquals("REWARDED", result.getStatus());

        Assertions.assertEquals(Map.of(
                "ID_2_sga", new Reward(BigDecimal.valueOf(0.08))
        ), result.getRewards());
    }
}