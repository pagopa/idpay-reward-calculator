package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.KieBase;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.util.SerializationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class RewardContextHolderServiceImplTest {

    @Mock private KieContainerBuilderService kieContainerBuilderServiceMock;
    @Mock private DroolsRuleRepository droolsRuleRepositoryMock;
    @Mock private ApplicationEventPublisher applicationEventPublisherMock;
    @Mock private ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplateMock;
    @Value("${spring.redis.enabled}") private boolean isRedisCacheEnabled;

    private RewardContextHolderService rewardContextHolderService;

    private final KieBase expectedKieBase = Mockito.mock(KieBase.class);

    @BeforeEach
    void init(){
        Mockito.when(droolsRuleRepositoryMock.findAll()).thenReturn(Flux.empty());
        Mockito.when(kieContainerBuilderServiceMock.build(Mockito.any())).thenReturn(Mono.just(expectedKieBase));
        if (isRedisCacheEnabled) {
            Mockito.when(reactiveRedisTemplateMock.opsForValue().get(Mockito.anyString())).thenReturn(Mono.just(SerializationUtils.serialize(expectedKieBase)));
        }

        rewardContextHolderService = new RewardContextHolderServiceImpl(kieContainerBuilderServiceMock, droolsRuleRepositoryMock, applicationEventPublisherMock, reactiveRedisTemplateMock);
    }

    @Test
    void getKieContainer() {
        // When
        KieBase result = rewardContextHolderService.getRewardRulesKieBase();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(expectedKieBase, result);
    }

    @Test
    void testNotRetrieveInitiativeConfig(){
        String initiativeId="INITIATIVE-ID";
        Mockito.when(droolsRuleRepositoryMock.findById(Mockito.same(initiativeId))).thenReturn(Mono.empty());

        // When
        InitiativeConfig result = rewardContextHolderService.getInitiativeConfig(initiativeId);

        //Then
        Assertions.assertNull(result);
        Mockito.verify(droolsRuleRepositoryMock).findById(Mockito.same(initiativeId));
    }

    @Test
    void testRetrieveInitiativeConfig(){
        String initiativeId="INITIATIVE-ID";
        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        DroolsRule droolsRule = DroolsRule.builder().initiativeConfig(initiativeConfig).build();
        Mockito.when(droolsRuleRepositoryMock.findById(Mockito.same(initiativeId))).thenReturn(Mono.just(droolsRule));

        // When
        InitiativeConfig result = rewardContextHolderService.getInitiativeConfig(initiativeId);

        //Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepositoryMock).findById(Mockito.same(initiativeId));
    }

    @Test
    void testSetInitiativeConfig(){
        String initiativeId="INITIATIVE-ID";
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryBudget(BigDecimal.valueOf(100))
                .endDate(LocalDate.MAX)
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