package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImplTest;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl=DEBUG",
})
public class RewardRuleConsumerConfigTest extends BaseIntegrationTest {

    @SpyBean
    private KieContainerBuilderService kieContainerBuilderServiceSpy;
    @SpyBean
    private RewardContextHolderService rewardContextHolderService;
    @SpyBean
    private DroolsRuleRepository droolsRuleRepositorySpy;

    @Test
    void testRewardRuleBuilding(){
        int validRules=100; // use even number
        int notValidRules = errorUseCases.size();
        long maxWaitingMs = 30000;

        int[] expectedRules ={0};
        List<String> initiativePayloads = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validRules / 2, expectedRules));
        initiativePayloads.addAll(IntStream.range(0, notValidRules).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        initiativePayloads.addAll(buildValidPayloads(errorUseCases.size() + (validRules / 2) + notValidRules, validRules / 2, expectedRules));

        long timeStart=System.currentTimeMillis();
        initiativePayloads.forEach(i->publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i));
        long timePublishingEnd=System.currentTimeMillis();

        long countSaved = waitForDroolsRulesStored(validRules);
        long timeDroolsSavingCheckPublishingEnd=System.currentTimeMillis();

        int ruleBuiltSize = waitForKieContainerBuild(expectedRules[0]);
        long timeEnd=System.currentTimeMillis();

        Assertions.assertEquals(validRules, countSaved);
        Assertions.assertEquals(expectedRules[0], ruleBuiltSize);

        checkErrorsPublished(notValidRules, maxWaitingMs, errorUseCases);

        Mockito.verify(kieContainerBuilderServiceSpy, Mockito.atLeast(1)).buildAll();
        Mockito.verify(rewardContextHolderService, Mockito.atLeast(1)).setRewardRulesKieContainer(Mockito.any());

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) messages (from start): %d millis
                        Time spent to assert drools rule count (from previous check): %d millis
                        Time spent to assert kie container rules' size (from previous check): %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        The kieContainer has been built %d times
                        The %d initiative generated %d rules
                        ************************
                        """,
                initiativePayloads.size(),
                validRules,
                notValidRules,
                timePublishingEnd-timeStart,
                timeDroolsSavingCheckPublishingEnd-timePublishingEnd,
                timeEnd-timeDroolsSavingCheckPublishingEnd,
                timeEnd-timeStart,
                Mockito.mockingDetails(kieContainerBuilderServiceSpy).getInvocations().stream()
                        .filter(i->i.getMethod().getName().equals("buildAll")).count()-1, // 1 is due on startup
                validRules, expectedRules[0]
        );

        long timeCommitCheckStart = System.currentTimeMillis();
        Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicRewardRuleConsumer, groupIdRewardRuleConsumer, initiativePayloads.size());
        long timeCommitCheckEnd = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        ************************
                        """,
                timeCommitCheckEnd - timeCommitCheckStart,
                srcCommitOffsets
        );
    }

    private List<String> buildValidPayloads(int bias, int validRules, int[] expectedRules) {
        return IntStream.range(bias, bias + validRules)
                .mapToObj(InitiativeReward2BuildDTOFaker::mockInstance)
                .peek(i-> expectedRules[0] += calcDroolsRuleGenerated(i))
                .map(TestUtils::jsonSerializer)
                .toList();
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
        waitFor(()->(countSaved[0]=droolsRuleRepository.count().block()) >= N, ()->"Expected %d saved rules, read %d".formatted(N, countSaved[0]), 90, 1000);
        return countSaved[0];
    }

    private int waitForKieContainerBuild(int expectedRules) {return waitForKieContainerBuild(expectedRules, rewardContextHolderService);}
    public static int waitForKieContainerBuild(int expectedRules,RewardContextHolderService rewardContextHolderServiceSpy) {
        int[] ruleBuiltSize={0};
        waitFor(()->(ruleBuiltSize[0]=getRuleBuiltSize(rewardContextHolderServiceSpy)) >= expectedRules, ()->"Expected %d rules, read %d".formatted(expectedRules, ruleBuiltSize[0]), 60, 500);
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

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp errorUseCaseIdPatternMatch
    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"initiativeId\":\"id_0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_RULE_BUILD] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"initiativeId\":\"id_1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_RULE_BUILD] Unexpected JSON", jsonNotValid)
        ));


        final String errorWhenSavingUseCaseId = "id_%s_ERRORWHENSAVING".formatted(errorUseCases.size());
        String droolRuleSaveInError = TestUtils.jsonSerializer(InitiativeReward2BuildDTOFaker.mockInstanceBuilder(errorUseCases.size(), null, null)
                .initiativeId(errorWhenSavingUseCaseId)
                .build());
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(Mono.error(new RuntimeException("DUMMYEXCEPTION"))).when(droolsRuleRepositorySpy).save(Mockito.argThat(i->errorWhenSavingUseCaseId.equals(i.getId())));
                    return droolRuleSaveInError;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD_RULE_BUILD] An error occurred handling initiative", droolRuleSaveInError)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicRewardRuleConsumer, errorMessage, errorDescription, expectedPayload);
    }
    //endregion
}