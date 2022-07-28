package it.gov.pagopa.reward.service.build;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsRuleTransformerFacadeImpl;
import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRuleTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

class RewardRule2DroolsRuleServiceTest {

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

    private RewardRule2DroolsRuleService buildRewardRule2DroolsRule(boolean executeOnlineBuildCheck) {
        return new RewardRule2DroolsRuleServiceImpl(
                executeOnlineBuildCheck,
                new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)),
                new TrxCondition2DroolsRuleTransformerFacadeImpl(new TrxCondition2DroolsConditionTransformerFacadeImpl()),
                new TrxConsequence2DroolsRuleTransformerFacadeImpl(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl())
        );
    }

    @Test
    void testBuild() {
        // given
        InitiativeReward2BuildDTO dto = InitiativeReward2BuildDTOFaker.mockInstance(0);

        // when
        DroolsRule result = buildRewardRule2DroolsRule(true).apply(dto);

        // then
        checkResult(result);
    }

    @Test
    public void testBuildWithOnlineBuildCheck() {
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
                salience 3
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(((trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf("TUESDAY")) && ((trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(0,16,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(2,0,0,0)))))))
                then $trx.getRejectionReason().add("TRX_RULE_DAYOFWEEK_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-MCCFILTER"
                salience 6
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(mcc not in ("0897","MCC_0")))
                then $trx.getRejectionReason().add("TRX_RULE_MCCFILTER_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-DAILY-REWARDLIMITS"
                salience 2
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(true))
                then $trx.getRejectionReason().add("TRX_RULE_REWARDLIMITS_DAILY_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-WEEKLY-REWARDLIMITS"
                salience 2
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(true))
                then $trx.getRejectionReason().add("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-MONTHLY-REWARDLIMITS"
                salience 2
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(true))
                then $trx.getRejectionReason().add("TRX_RULE_REWARDLIMITS_MONTHLY_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-YEARLY-REWARDLIMITS"
                salience 2
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(true))
                then $trx.getRejectionReason().add("TRX_RULE_REWARDLIMITS_YEARLY_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-THRESHOLD"
                salience 5
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(amount >= new java.math.BigDecimal("0") && amount <= new java.math.BigDecimal("10")))
                then $trx.getRejectionReason().add("TRX_RULE_THRESHOLD_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-TRXCOUNT"
                salience 1
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(true))
                then $trx.getRejectionReason().add("TRX_RULE_TRXCOUNT_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-REWARDGROUP"
                salience 4
                agenda-group "ID_0_ssx"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(((amount >= new java.math.BigDecimal("0") && amount <= new java.math.BigDecimal("5")))))
                then $trx.getRejectionReason().add("TRX_RULE_REWARDGROUP_FAIL");
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-REWARDGROUPS"
                salience -1
                agenda-group "ID_0_ssx"
                when $trx: it.gov.pagopa.reward.model.RewardTransaction(rejectionReason.size() == 0)
                then $trx.getRewards().put("ID_0_ssx", $trx.getAmount().multiply(($trx.getAmount().compareTo(new java.math.BigDecimal("0"))>=0 && $trx.getAmount().compareTo(new java.math.BigDecimal("5"))<=0)?new java.math.BigDecimal("0.0000"):java.math.BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_DOWN));
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-REWARDLIMITS-DAILY-"
                salience -2
                agenda-group "ID_0_ssx"
                when $trx: it.gov.pagopa.reward.model.RewardTransaction(rejectionReason.size() == 0)
                then $trx.getRewards().put("ID_0_ssx", java.math.BigDecimal.ZERO);
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-REWARDLIMITS-WEEKLY-"
                salience -2
                agenda-group "ID_0_ssx"
                when $trx: it.gov.pagopa.reward.model.RewardTransaction(rejectionReason.size() == 0)
                then $trx.getRewards().put("ID_0_ssx", java.math.BigDecimal.ZERO);
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-REWARDLIMITS-MONTHLY-"
                salience -2
                agenda-group "ID_0_ssx"
                when $trx: it.gov.pagopa.reward.model.RewardTransaction(rejectionReason.size() == 0)
                then $trx.getRewards().put("ID_0_ssx", java.math.BigDecimal.ZERO);
                end
                                
                rule "ID_0_ssx-NAME_0_vnj-REWARDLIMITS-YEARLY-"
                salience -2
                agenda-group "ID_0_ssx"
                when $trx: it.gov.pagopa.reward.model.RewardTransaction(rejectionReason.size() == 0)
                then $trx.getRewards().put("ID_0_ssx", java.math.BigDecimal.ZERO);
                end
                                
                """);

        /*expected.setInitiativeConfig(new InitiativeConfig("ID", TODO to fix after merge with IRER-99
                LocalDate.of(2021,1,1),LocalDate.of(2025,12,1),
                "PDND_TOKEN", List.of("ISEE", "BIRTHDATE"), new BigDecimal(100000.00), new BigDecimal(1000.00)));*/

        Assertions.assertEquals(expected, result);
    }

}
