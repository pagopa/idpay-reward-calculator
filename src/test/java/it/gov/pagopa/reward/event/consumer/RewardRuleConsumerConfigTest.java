package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImplTest;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "app.rules.cache.refresh-ms-rate=60000",
        "logging.level.it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl=DEBUG",
})
public class RewardRuleConsumerConfigTest extends BaseIntegrationTest {

    @SpyBean
    private KieContainerBuilderService kieContainerBuilderServiceSpy;
    @SpyBean
    private RewardContextHolderService rewardContextHolderService;

    @Test
    void testRewardRuleBuilding(){
        int N=100;
        int[] expectedRules ={0};
        List<InitiativeReward2BuildDTO> initiatives = IntStream.range(0,N)
                .mapToObj(InitiativeReward2BuildDTOFaker::mockInstance)
                .peek(i-> expectedRules[0] += calcDroolsRuleGenerated(i))
                .toList();

        long timeStart=System.currentTimeMillis();
        initiatives.forEach(i->publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i));
        long timePublishingEnd=System.currentTimeMillis();

        long countSaved = waitForDroolsRulesStored(N);
        long timeDroolsSavingCheckPublishingEnd=System.currentTimeMillis();

        int ruleBuiltSize = waitForKieContainerBuild(expectedRules[0]);
        long timeEnd=System.currentTimeMillis();

        Assertions.assertEquals(N, countSaved);
        Assertions.assertEquals(expectedRules[0], ruleBuiltSize);

        Mockito.verify(kieContainerBuilderServiceSpy, Mockito.atLeast(2)).buildAll(); // +1 due to refresh at startup
        Mockito.verify(rewardContextHolderService, Mockito.atLeast(1)).setRewardRulesKieContainer(Mockito.any());

        System.out.printf("""
            ************************
            Time spent to send %d messages (from start): %d millis
            Time spent to assert drools rule count (from previous check): %d millis
            Time spent to assert kie container rules' size (from previous check): %d millis
            ************************
            Test Completed in %d millis
            ************************
            The kieContainer has been built %d times
            The %d initiative generated %d rules
            ************************
            """,
                N,
                timePublishingEnd-timeStart,
                timeDroolsSavingCheckPublishingEnd-timePublishingEnd,
                timeEnd-timeDroolsSavingCheckPublishingEnd,
                timeEnd-timeStart,
                Mockito.mockingDetails(kieContainerBuilderServiceSpy).getInvocations().stream()
                        .filter(i->i.getMethod().getName().equals("buildAll")).count()-1, // 1 is due on startup
                N, expectedRules[0]
        );
    }

    public static int calcDroolsRuleGenerated(InitiativeReward2BuildDTO i) {
        return (i.getTrxRule().getDaysOfWeek() != null? 1 : 0) +
        (i.getTrxRule().getThreshold() != null? 1 : 0) +
        (i.getTrxRule().getMccFilter() != null? 1 : 0) +
        (i.getTrxRule().getTrxCount() != null? 1 : 0) +
        (i.getTrxRule().getRewardLimits() != null? i.getTrxRule().getRewardLimits().size()*2 : 0) + // x2 because each rule generates a condition and a consequence
        (i.getRewardRule() instanceof RewardGroupsDTO ? 2 : 1); // RewardGroupsDTO 2 because it generates a condition and a consequence
    }

    private long waitForDroolsRulesStored(int N) {
        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=droolsRuleRepository.count().block()) >= N, ()->"Expected %d saved rules, read %d".formatted(N, countSaved[0]), 25, 1000);
        return countSaved[0];
    }

    private int waitForKieContainerBuild(int expectedRules) {return waitForKieContainerBuild(expectedRules, rewardContextHolderService);}
    public static int waitForKieContainerBuild(int expectedRules,RewardContextHolderService rewardContextHolderServiceSpy) {
        int[] ruleBuiltSize={0};
        waitFor(()->(ruleBuiltSize[0]=getRuleBuiltSize(rewardContextHolderServiceSpy)) >= expectedRules, ()->"Expected %d rules, read %d".formatted(expectedRules, ruleBuiltSize[0]), 40, 500);
        return ruleBuiltSize[0];
    }

    public static int getRuleBuiltSize(RewardContextHolderService rewardContextHolderServiceSpy) {
        KieContainer kieContainer = rewardContextHolderServiceSpy.getRewardRulesKieContainer();
        if (kieContainer == null) {
            return 0;
        } else {
            return KieContainerBuilderServiceImplTest.getRuleBuiltSize(kieContainer);
        }
    }

}