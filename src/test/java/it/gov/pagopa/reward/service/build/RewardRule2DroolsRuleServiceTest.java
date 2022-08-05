package it.gov.pagopa.reward.service.build;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsRuleTransformerFacadeImpl;
import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRuleTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.mapper.InitiativeReward2BuildDTO2ConfigMapper;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDroolsDtoFaker;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.command.CommandFactory;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.*;

public class RewardRule2DroolsRuleServiceTest {

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.drools.compiler.kie.builder.impl")).setLevel(Level.WARN);
    }

    private final RewardRule2DroolsRuleService rewardRule2DroolsRuleService;

    public RewardRule2DroolsRuleServiceTest() {
        this.rewardRule2DroolsRuleService = buildRewardRule2DroolsRule(false);
    }

    public static RewardRule2DroolsRuleService buildRewardRule2DroolsRule(boolean executeOnlineBuildCheck) {
        return new RewardRule2DroolsRuleServiceImpl(
                executeOnlineBuildCheck,
                new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)),
                new TrxCondition2DroolsRuleTransformerFacadeImpl(new TrxCondition2DroolsConditionTransformerFacadeImpl()),
                new TrxConsequence2DroolsRuleTransformerFacadeImpl(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl()),
                new InitiativeReward2BuildDTO2ConfigMapper()
        );
    }

    @Test
    void testBuildEmpty() {
        // given
        InitiativeReward2BuildDTO dto = new InitiativeReward2BuildDTO();
        dto.setTrxRule(new InitiativeTrxConditions());
        dto.setGeneral(InitiativeGeneralDTO.builder().budget(BigDecimal.TEN).build());

        // when
        DroolsRule result = buildRewardRule2DroolsRule(true).apply(dto);

        // then
        Assertions.assertEquals("package it.gov.pagopa.reward.drools.buildrules;\n\n", result.getRule());
    }

    @Test
    void testBuild() {
        // given
        InitiativeReward2BuildDTO dto = InitiativeReward2BuildDTOFaker.mockInstance(0);

        // when
        DroolsRule result = buildRewardRule2DroolsRule(true).apply(dto);

        // then
        checkResult(result);

        executeRule(result);
    }

    @Test
    void testBuildWithOnlineBuildCheck() {
        // given
        InitiativeReward2BuildDTO dto = InitiativeReward2BuildDTOFaker.mockInstance(0);

        // when
        DroolsRule result = rewardRule2DroolsRuleService.apply(dto);

        // then
        checkResult(result);
    }

    private void checkResult(DroolsRule result) {
        DroolsRule expected = new DroolsRule();
        expected.setId("ID_0_ssx");
        expected.setName("ID_0_ssx-NAME_0_vnj");
        expected.setRule("""
                package it.gov.pagopa.reward.drools.buildrules;
                                        
                rule "ID_0_ssx-NAME_0_vnj-DAYOFWEEK"
                salience 1
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !(((trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf("TUESDAY")) && ((trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(0,16,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(2,0,0,0)))))))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_DAYOFWEEK_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-MCCFILTER"
                salience 4
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !(mcc not in ("0897","MCC_0")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_MCCFILTER_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-DAILY-REWARDLIMITS"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !($initiativeCounters.getDailyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.getDayDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("10")) < 0))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_DAILY_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-WEEKLY-REWARDLIMITS"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !($initiativeCounters.getWeeklyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.getWeekDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("70")) < 0))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-MONTHLY-REWARDLIMITS"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !($initiativeCounters.getMonthlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.getMonthDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("300")) < 0))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_MONTHLY_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-YEARLY-REWARDLIMITS"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !($initiativeCounters.getYearlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.getYearDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("3650")) < 0))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_YEARLY_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-THRESHOLD"
                salience 3
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !(amount >= new java.math.BigDecimal("0") && amount <= new java.math.BigDecimal("10")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_THRESHOLD_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-TRXCOUNT"
                salience 5
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !($initiativeCounters.trxNumber >= new java.lang.Long("-1") && $initiativeCounters.trxNumber <= new java.lang.Long("9")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_TRXCOUNT_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-REWARDVALUE"
                salience -1
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then $trx.getRewards().put("ID_0_ssx", new it.gov.pagopa.reward.dto.Reward($trx.getAmount().multiply(new java.math.BigDecimal("0.0023")).setScale(2, java.math.RoundingMode.HALF_DOWN)));
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-DAILY-REWARDLIMITS-CAP"
                salience -2
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then\s
                   it.gov.pagopa.reward.dto.Reward reward = $trx.getRewards().get("ID_0_ssx");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("ID_0_ssx").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("10").subtract($initiativeCounters.getDailyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.getDayDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward()))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.setDailyCapped(true);
                         reward.setWeeklyCapped(false);
                         reward.setMonthlyCapped(false);
                         reward.setYearlyCapped(false);
                      }
                   }
                end
                
                rule "ID_0_ssx-NAME_0_vnj-WEEKLY-REWARDLIMITS-CAP"
                salience -2
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then\s
                   it.gov.pagopa.reward.dto.Reward reward = $trx.getRewards().get("ID_0_ssx");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("ID_0_ssx").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("70").subtract($initiativeCounters.getWeeklyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.getWeekDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward()))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.setWeeklyCapped(true);
                         reward.setDailyCapped(false);
                         reward.setMonthlyCapped(false);
                         reward.setYearlyCapped(false);
                      }
                   }
                end
                
                rule "ID_0_ssx-NAME_0_vnj-MONTHLY-REWARDLIMITS-CAP"
                salience -2
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then\s
                   it.gov.pagopa.reward.dto.Reward reward = $trx.getRewards().get("ID_0_ssx");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("ID_0_ssx").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("300").subtract($initiativeCounters.getMonthlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.getMonthDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward()))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.setMonthlyCapped(true);
                         reward.setDailyCapped(false);
                         reward.setWeeklyCapped(false);
                         reward.setYearlyCapped(false);
                      }
                   }
                end
                
                rule "ID_0_ssx-NAME_0_vnj-YEARLY-REWARDLIMITS-CAP"
                salience -2
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.InitiativeCounters("ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then\s
                   it.gov.pagopa.reward.dto.Reward reward = $trx.getRewards().get("ID_0_ssx");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("ID_0_ssx").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("3650").subtract($initiativeCounters.getYearlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.getYearDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward()))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.setYearlyCapped(true);
                         reward.setDailyCapped(false);
                         reward.setWeeklyCapped(false);
                         reward.setMonthlyCapped(false);
                      }
                   }
                end
                                
                """);

        expected.setInitiativeConfig(InitiativeConfig.builder()
                .initiativeId(expected.getId())
                .budget(BigDecimal.valueOf(10000L))
                .dailyThreshold(true)
                .weeklyThreshold(true)
                .monthlyThreshold(true)
                .yearlyThreshold(true)
                .build());

        Assertions.assertEquals(expected, result);
    }

    private KieContainer buildRule(DroolsRule dr) {
        try {
            return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(dr)).block();
        } catch (RuntimeException e) {
            System.out.printf("Something gone wrong building the rule: %s%n", dr.getRule());
            throw e;
        }
    }


    private void executeRule(DroolsRule dr) {
        KieContainer kieContainer = buildRule(dr);
        TransactionDroolsDTO trx = TransactionDroolsDtoFaker.mockInstance(0);
        executeRule(dr.getId(), trx, false, null, kieContainer);
        Assertions.assertEquals(
                Map.of(
                        dr.getId(), List.of("TRX_RULE_THRESHOLD_FAIL", "TRX_RULE_DAYOFWEEK_FAIL")
                ), trx.getInitiativeRejectionReasons());

        trx.setInitiativeRejectionReasons(new HashMap<>());
        executeRule(dr.getId(), trx, true, null, kieContainer);
        Assertions.assertEquals(
                Map.of(
                        dr.getId(), List.of("TRX_RULE_THRESHOLD_FAIL")
                ), trx.getInitiativeRejectionReasons());
    }

    public static void executeRule(String initiativeId, TransactionDroolsDTO trx, boolean shortCircuited, UserInitiativeCounters counters, KieContainer kieContainer) {
        RuleEngineConfig ruleEngineConfig = new RuleEngineConfig();
        ruleEngineConfig.setShortCircuitConditions(shortCircuited);

        if(counters==null){
            counters = new UserInitiativeCounters(trx.getHpan(), new HashMap<>());
        }

        @SuppressWarnings("unchecked")
        List<Command<?>> commands = Arrays.asList(
                CommandFactory.newInsert(ruleEngineConfig),
                CommandFactory.newInsert(counters),
                CommandFactory.newInsert(trx),
                new AgendaGroupSetFocusCommand(initiativeId)
        );
        kieContainer.newStatelessKieSession().execute(CommandFactory.newBatchExecution(commands));
    }
}
