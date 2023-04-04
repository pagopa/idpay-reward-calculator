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
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDroolsDtoFaker;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.command.Command;
import org.kie.internal.command.CommandFactory;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 ******************
 For any change necessary on this test consider if update "ruleVersion" value setted in it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceImpl.apply
 ******************
*/
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
        Assertions.assertEquals("package it.gov.pagopa.reward.drools.buildrules;\n\n// null\n// ruleVersion: 20230404\n\n", result.getRule());
    }

    @Test
    void testBuild() {
        // given
        InitiativeReward2BuildDTO dto = InitiativeReward2BuildDTOFaker.mockInstance(0);

        // when
        DroolsRule result = buildRewardRule2DroolsRule(true).apply(dto);

        // then
        checkResult(result, dto);

        executeRule(result);
    }

    @Test
    void testBuildWithOnlineBuildCheck() {
        // given
        InitiativeReward2BuildDTO dto = InitiativeReward2BuildDTOFaker.mockInstance(0);

        // when
        DroolsRule result = rewardRule2DroolsRuleService.apply(dto);

        // then
        checkResult(result, dto);
    }

    private void checkResult(DroolsRule result, InitiativeReward2BuildDTO dto) {
        DroolsRule expected = new DroolsRule();
        expected.setId("ID_0_ssx");
        expected.setName("NAME_0_vnj");
        expected.setRule("""
                package it.gov.pagopa.reward.drools.buildrules;
                
                // NAME_0_vnj
                // ruleVersion: 20230404
                
                rule "ID_0_ssx-DAYOFWEEK"
                salience 1
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !(((trxChargeDate.dayOfWeek in (java.time.DayOfWeek.valueOf("TUESDAY")) && ((trxChargeDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(0,16,0,0) && trxChargeDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(2,0,0,0)))))))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_DAYOFWEEK_FAIL");
                end
                                
                rule "ID_0_ssx-MCCFILTER"
                salience 4
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !(mcc not in ("0897","MCC_0")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_MCCFILTER_FAIL");
                end
                                
                rule "ID_0_ssx-DAILY-REWARDLIMITS"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !((it.gov.pagopa.reward.enums.OperationType.valueOf("REFUND").equals($trx.getOperationTypeTranscoded()) || $userInitiativeCounters.getDailyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getDayDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("10")) < 0)))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_DAILY_FAIL");
                end
                                
                rule "ID_0_ssx-WEEKLY-REWARDLIMITS"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !((it.gov.pagopa.reward.enums.OperationType.valueOf("REFUND").equals($trx.getOperationTypeTranscoded()) || $userInitiativeCounters.getWeeklyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getWeekDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("70")) < 0)))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL");
                end
                                
                rule "ID_0_ssx-MONTHLY-REWARDLIMITS"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !((it.gov.pagopa.reward.enums.OperationType.valueOf("REFUND").equals($trx.getOperationTypeTranscoded()) || $userInitiativeCounters.getMonthlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getMonthDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("300")) < 0)))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_MONTHLY_FAIL");
                end
                                
                rule "ID_0_ssx-YEARLY-REWARDLIMITS"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !((it.gov.pagopa.reward.enums.OperationType.valueOf("REFUND").equals($trx.getOperationTypeTranscoded()) || $userInitiativeCounters.getYearlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getYearDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("3650")) < 0)))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_YEARLY_FAIL");
                end
                                
                rule "ID_0_ssx-THRESHOLD"
                salience 3
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !(effectiveAmount >= new java.math.BigDecimal("0") && effectiveAmount <= new java.math.BigDecimal("10")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_THRESHOLD_FAIL");
                end
                                
                rule "ID_0_ssx-TRXCOUNT"
                salience 5
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("ID_0_ssx") == null, !($userInitiativeCounters.trxNumber >= new java.lang.Long("-1") && $userInitiativeCounters.trxNumber <= new java.lang.Long("9")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("ID_0_ssx",k->new java.util.ArrayList<>()).add("TRX_RULE_TRXCOUNT_FAIL");
                end
                                
                rule "ID_0_ssx-REWARDVALUE"
                salience -1
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then $trx.getRewards().put("ID_0_ssx", new it.gov.pagopa.reward.dto.trx.Reward("ID_0_ssx","ORGANIZATIONID_0",$trx.getEffectiveAmount().multiply(new java.math.BigDecimal("0.0023")).setScale(2, java.math.RoundingMode.HALF_DOWN)));
                end
                                
                rule "ID_0_ssx-DAILY-REWARDLIMITS-CAP"
                salience -2
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then\s
                   it.gov.pagopa.reward.dto.trx.Reward reward = $trx.getRewards().get("ID_0_ssx");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("ID_0_ssx").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("10").subtract($userInitiativeCounters.getDailyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getDayDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward().subtract($trx.getRefundInfo()!=null && $trx.getRefundInfo().getPreviousRewards()!=null && $trx.getRefundInfo().getPreviousRewards().get("ID_0_ssx")!=null ? $trx.getRefundInfo().getPreviousRewards().get("ID_0_ssx").getAccruedReward() : java.math.BigDecimal.ZERO)))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.setDailyCapped(true);
                         reward.setWeeklyCapped(false);
                         reward.setMonthlyCapped(false);
                         reward.setYearlyCapped(false);
                      }
                   }
                end
                
                rule "ID_0_ssx-WEEKLY-REWARDLIMITS-CAP"
                salience -2
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then\s
                   it.gov.pagopa.reward.dto.trx.Reward reward = $trx.getRewards().get("ID_0_ssx");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("ID_0_ssx").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("70").subtract($userInitiativeCounters.getWeeklyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getWeekDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward().subtract($trx.getRefundInfo()!=null && $trx.getRefundInfo().getPreviousRewards()!=null && $trx.getRefundInfo().getPreviousRewards().get("ID_0_ssx")!=null ? $trx.getRefundInfo().getPreviousRewards().get("ID_0_ssx").getAccruedReward() : java.math.BigDecimal.ZERO)))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.setWeeklyCapped(true);
                         reward.setDailyCapped(false);
                         reward.setMonthlyCapped(false);
                         reward.setYearlyCapped(false);
                      }
                   }
                end
                
                rule "ID_0_ssx-MONTHLY-REWARDLIMITS-CAP"
                salience -2
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then\s
                   it.gov.pagopa.reward.dto.trx.Reward reward = $trx.getRewards().get("ID_0_ssx");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("ID_0_ssx").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("300").subtract($userInitiativeCounters.getMonthlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getMonthDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward().subtract($trx.getRefundInfo()!=null && $trx.getRefundInfo().getPreviousRewards()!=null && $trx.getRefundInfo().getPreviousRewards().get("ID_0_ssx")!=null ? $trx.getRefundInfo().getPreviousRewards().get("ID_0_ssx").getAccruedReward() : java.math.BigDecimal.ZERO)))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.setMonthlyCapped(true);
                         reward.setDailyCapped(false);
                         reward.setWeeklyCapped(false);
                         reward.setYearlyCapped(false);
                      }
                   }
                end
                
                rule "ID_0_ssx-YEARLY-REWARDLIMITS-CAP"
                salience -2
                agenda-group "ID_0_ssx"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("ID_0_ssx", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "ID_0_ssx"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("ID_0_ssx") == null)
                then\s
                   it.gov.pagopa.reward.dto.trx.Reward reward = $trx.getRewards().get("ID_0_ssx");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("ID_0_ssx").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("3650").subtract($userInitiativeCounters.getYearlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getYearDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward().subtract($trx.getRefundInfo()!=null && $trx.getRefundInfo().getPreviousRewards()!=null && $trx.getRefundInfo().getPreviousRewards().get("ID_0_ssx")!=null ? $trx.getRefundInfo().getPreviousRewards().get("ID_0_ssx").getAccruedReward() : java.math.BigDecimal.ZERO)))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.setYearlyCapped(true);
                         reward.setDailyCapped(false);
                         reward.setWeeklyCapped(false);
                         reward.setMonthlyCapped(false);
                      }
                   }
                end
                   
                rule "ID_0_ssx-TRXCOUNT-REWARD"
                salience -3
                agenda-group "ID_0_ssx"
                when
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval(java.util.List.of("TRX_RULE_TRXCOUNT_FAIL").equals($trx.getInitiativeRejectionReasons().get("ID_0_ssx")))
                then $trx.getRewards().put("ID_0_ssx", new it.gov.pagopa.reward.dto.trx.Reward("ID_0_ssx","ORGANIZATIONID_0",java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.UNNECESSARY)));
                end
                                
                """);

        expected.setInitiativeConfig(InitiativeConfig.builder()
                .initiativeId(expected.getId())
                .initiativeName("NAME_0_vnj")
                .organizationId("ORGANIZATIONID_0")
                .beneficiaryBudget(BigDecimal.valueOf(477.32))
                .dailyThreshold(true)
                .weeklyThreshold(true)
                .monthlyThreshold(true)
                .yearlyThreshold(true)
                .trxRule(dto.getTrxRule())
                .rewardRule(dto.getRewardRule())
                .build());

        expected.setRuleVersion("20230404");
        expected.setUpdateDate(result.getUpdateDate());

        Assertions.assertEquals(expected, result);
    }

    private KieBase buildRule(DroolsRule dr) {
        try {
            return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(dr)).block();
        } catch (RuntimeException e) {
            System.out.printf("Something gone wrong building the rule: %s%n", dr.getRule());
            throw e;
        }
    }


    private void executeRule(DroolsRule dr) {
        KieBase kieBase = buildRule(dr);
        TransactionDroolsDTO trx = TransactionDroolsDtoFaker.mockInstance(0);
        executeRule(dr.getId(), trx, false, null, kieBase);
        Assertions.assertEquals(
                Map.of(
                        dr.getId(), List.of("TRX_RULE_THRESHOLD_FAIL", "TRX_RULE_DAYOFWEEK_FAIL")
                ), trx.getInitiativeRejectionReasons());

        trx.setInitiativeRejectionReasons(new HashMap<>());
        executeRule(dr.getId(), trx, true, null, kieBase);
        Assertions.assertEquals(
                Map.of(
                        dr.getId(), List.of("TRX_RULE_THRESHOLD_FAIL")
                ), trx.getInitiativeRejectionReasons());
    }

    public static void executeRule(String initiativeId, TransactionDroolsDTO trx, boolean shortCircuited, UserInitiativeCountersWrapper counters, KieBase kieBase) {
        RuleEngineConfig ruleEngineConfig = new RuleEngineConfig();
        ruleEngineConfig.setShortCircuitConditions(shortCircuited);

        if(counters==null){
            counters = new UserInitiativeCountersWrapper(trx.getHpan(), new HashMap<>());
        }

        @SuppressWarnings("unchecked")
        List<Command<?>> commands = Arrays.asList(
                CommandFactory.newInsert(ruleEngineConfig),
                CommandFactory.newInsert(counters),
                CommandFactory.newInsert(trx),
                new AgendaGroupSetFocusCommand(initiativeId)
        );
        kieBase.newStatelessKieSession().execute(CommandFactory.newBatchExecution(commands));
    }
}
