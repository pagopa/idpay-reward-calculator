package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class RewardContextHolderServiceImplTest {

    @Test
    void getKieContainer() {
        // Given
        KieContainerBuilderService kieContainerBuilderService = Mockito.mock(KieContainerBuilderService.class);
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);

        KieContainer kieContainer = Mockito.mock(KieContainer.class);
        Mockito.when(kieContainerBuilderService.buildAll()).thenReturn(Mono.just(kieContainer));

        RewardContextHolderService rewardContextHolderService = new RewardContextHolderServiceImpl(kieContainerBuilderService, droolsRuleRepository);

        // When
        KieContainer result = rewardContextHolderService.getRewardRulesKieContainer();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(kieContainer, result);


    }

    @Test
    void testNotRetrieveInitiativeConfig(){
        KieContainerBuilderService kieContainerBuilderService = Mockito.mock(KieContainerBuilderService.class);
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);

        KieContainer kieContainer = Mockito.mock(KieContainer.class);
        Mockito.when(kieContainerBuilderService.buildAll()).thenReturn(Mono.just(kieContainer));
        RewardContextHolderService rewardContextHolderService = new RewardContextHolderServiceImpl(kieContainerBuilderService, droolsRuleRepository);

        String initiativeId="INITIATIVE-ID";
        Mockito.when(droolsRuleRepository.findById(Mockito.same(initiativeId))).thenReturn(Mono.empty());

        // When
        InitiativeConfig result = rewardContextHolderService.getInitiativeConfig(initiativeId);

        //Then
        Assertions.assertNull(result);
        Mockito.verify(droolsRuleRepository).findById(Mockito.same(initiativeId));
    }

    @Test
    void testRetrieveInitiativeConfig(){
        KieContainerBuilderService kieContainerBuilderService = Mockito.mock(KieContainerBuilderService.class);
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);

        KieContainer kieContainer = Mockito.mock(KieContainer.class);
        Mockito.when(kieContainerBuilderService.buildAll()).thenReturn(Mono.just(kieContainer));
        RewardContextHolderService rewardContextHolderService = new RewardContextHolderServiceImpl(kieContainerBuilderService, droolsRuleRepository);

        String initiativeId="INITIATIVE-ID";
        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        DroolsRule droolsRule = DroolsRule.builder().initiativeConfig(initiativeConfig).build();
        Mockito.when(droolsRuleRepository.findById(Mockito.same(initiativeId))).thenReturn(Mono.just(droolsRule));

        // When
        InitiativeConfig result = rewardContextHolderService.getInitiativeConfig(initiativeId);

        //Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepository).findById(Mockito.same(initiativeId));
    }

    @Test
    void testSetInitiativeConfig(){
        KieContainerBuilderService kieContainerBuilderService = Mockito.mock(KieContainerBuilderService.class);
        DroolsRuleRepository droolsRuleRepository = Mockito.mock(DroolsRuleRepository.class);

        KieContainer kieContainer = Mockito.mock(KieContainer.class);
        Mockito.when(kieContainerBuilderService.buildAll()).thenReturn(Mono.just(kieContainer));
        RewardContextHolderService rewardContextHolderService = new RewardContextHolderServiceImpl(kieContainerBuilderService, droolsRuleRepository);

        String initiativeId="INITIATIVE-ID";
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .dailyThreshold(true)
                .monthlyThreshold(false)
                .yearlyThreshold(false).build();


        // When
        rewardContextHolderService.setInitiativeConfig(initiativeConfig);
        InitiativeConfig result = rewardContextHolderService.getInitiativeConfig(initiativeId);

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
    }
}