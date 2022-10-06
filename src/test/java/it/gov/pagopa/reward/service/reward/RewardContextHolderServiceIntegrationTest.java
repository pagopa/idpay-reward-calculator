package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

class RewardContextHolderServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KieContainerBuilderService kieContainerBuilderServiceMock;
    @Autowired
    private DroolsRuleRepository droolsRuleRepositoryMock;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisherMock;
    @Autowired
    private ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplateMock;


    @Test
    void testKieContainerBuildWithRedisCache() {

        RewardContextHolderService rewardContextHolderService = new RewardContextHolderServiceImpl(
                kieContainerBuilderServiceMock,
                droolsRuleRepositoryMock,
                applicationEventPublisherMock,
                reactiveRedisTemplateMock
        );

        int ruleBuiltSize = RewardRuleConsumerConfigTest.getRuleBuiltSize(rewardContextHolderService);

        DroolsRule dr = new DroolsRule();
        dr.setId("NAME");
        dr.setName("RULE");
        dr.setRule("""
                package %s;
                                
                rule "%s"
                agenda-group "%s"
                when $trx
                then $trx.setRejectionReasons(List.of("OK"));
                end
                """.formatted(
                        KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE,
                        dr.getRule(),
                        dr.getName()
                )
        );

        Assertions.assertEquals(0, ruleBuiltSize);
    }

}
