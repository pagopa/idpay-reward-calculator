package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.repository.secondary.DroolsRuleRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.api.KieBase;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.util.SerializationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class RewardContextHolderServiceImplTest {

    @Mock private ApplicationAvailability applicationAvailabilityMock;
    @Mock private GenericApplicationContext applicationContextMock;
    @Mock private KieContainerBuilderService kieContainerBuilderServiceMock;
    @Mock private DroolsRuleRepository droolsRuleRepositoryMock;
    @Mock private ApplicationEventPublisher applicationEventPublisherMock;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplateMock;
    private RewardContextHolderService rewardContextHolderService;

    private final KieBase expectedKieBase = new KieContainerBuilderServiceImpl(droolsRuleRepositoryMock).build(Flux.empty()).block();

    void init(boolean isRedisCacheEnabled){
        configureMocks(isRedisCacheEnabled);
        buildService(isRedisCacheEnabled);
    }

    private void configureMocks(boolean isRedisCacheEnabled) {
        Assertions.assertNotNull(expectedKieBase);

        Mockito.when(droolsRuleRepositoryMock.findAll()).thenReturn(Flux.empty());
        Mockito.when(kieContainerBuilderServiceMock.build(Mockito.any())).thenReturn(Mono.just(expectedKieBase));

        if (isRedisCacheEnabled) {
            byte[] expectedKieBaseSerialized = SerializationUtils.serialize(expectedKieBase);
            Assertions.assertNotNull(expectedKieBaseSerialized);
            Mockito.when(reactiveRedisTemplateMock.opsForValue().get(Mockito.anyString())).thenReturn(Mono.just(expectedKieBaseSerialized));
        }
    }

    private void buildService(boolean isRedisCacheEnabled) {
        rewardContextHolderService = new RewardContextHolderServiceImpl(applicationAvailabilityMock, applicationContextMock, kieContainerBuilderServiceMock, droolsRuleRepositoryMock, applicationEventPublisherMock, reactiveRedisTemplateMock, isRedisCacheEnabled, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getKieContainer(boolean isRedisCacheEnabled) {
        // Given
        init(isRedisCacheEnabled);

        // When
        KieBase result = rewardContextHolderService.getRewardRulesKieBase();

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Collections.emptySet(), rewardContextHolderService.getRewardRulesKieInitiativeIds());
        if (!isRedisCacheEnabled) {
            Assertions.assertSame(expectedKieBase, result);
        }

        checkReadiness(ReadinessState.ACCEPTING_TRAFFIC);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testNotRetrieveInitiativeConfig(boolean isRedisCacheEnabled){
        init(isRedisCacheEnabled);

        String initiativeId="INITIATIVE-ID";
        Mockito.when(droolsRuleRepositoryMock.findById(Mockito.same(initiativeId))).thenReturn(Mono.empty());

        // When
        InitiativeConfig result = rewardContextHolderService.getInitiativeConfig(initiativeId).block();

        //Then
        Assertions.assertNull(result);
        Mockito.verify(droolsRuleRepositoryMock).findById(Mockito.same(initiativeId));

        checkReadiness(ReadinessState.ACCEPTING_TRAFFIC);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testRetrieveInitiativeConfig(boolean isRedisCacheEnabled){
        init(isRedisCacheEnabled);

        String initiativeId="INITIATIVE-ID";
        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        DroolsRule droolsRule = DroolsRule.builder().initiativeConfig(initiativeConfig).build();
        Mockito.when(droolsRuleRepositoryMock.findById(Mockito.same(initiativeId))).thenReturn(Mono.just(droolsRule));

        // When
        InitiativeConfig result = rewardContextHolderService.getInitiativeConfig(initiativeId).block();

        //Then
        Assertions.assertNotNull(result);
        Mockito.verify(droolsRuleRepositoryMock).findById(Mockito.same(initiativeId));

        checkReadiness(ReadinessState.ACCEPTING_TRAFFIC);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSetInitiativeConfig(boolean isRedisCacheEnabled){
        init(isRedisCacheEnabled);

        String initiativeId="INITIATIVE-ID";
        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .initiativeName("NAME")
                .organizationId("ORGANIZATIONID")
                .beneficiaryBudgetCents(100_00L)
                .startDate(LocalDate.MIN)
                .endDate(LocalDate.MAX)
                .dailyThreshold(true)
                .monthlyThreshold(false)
                .yearlyThreshold(false)
                .trxRule(new InitiativeTrxConditions())
                .rewardRule(new RewardValueDTO())
                .initiativeRewardType(InitiativeRewardType.REFUND)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .build();


        // When
        rewardContextHolderService.setInitiativeConfig(initiativeConfig);
        InitiativeConfig result = rewardContextHolderService.getInitiativeConfig(initiativeId).block();

        //Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);

        checkReadiness(ReadinessState.ACCEPTING_TRAFFIC);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testFailingContextStart(boolean isRedisCacheEnabled){
        int[] counter = {0};
        Mono<?> monoError = Mono.defer(() -> {
            counter[0]++;
            return Mono.error(new IllegalStateException("DUMMYEXCEPTION"));
        });

        configureMocks(isRedisCacheEnabled);
        if(isRedisCacheEnabled){
            //noinspection unchecked
            Mockito.when(reactiveRedisTemplateMock.opsForValue().get(Mockito.anyString())).thenReturn((Mono<byte[]>) monoError);
        } else {
            //noinspection unchecked
            Mockito.when(kieContainerBuilderServiceMock.build(Mockito.notNull())).thenReturn((Mono<KieBase>) monoError);
        }

        buildService(isRedisCacheEnabled);

        TestUtils.waitFor(()-> {
            Mockito.verify(applicationContextMock).close();
            Assertions.assertEquals(4, counter[0]);
            checkReadiness(ReadinessState.REFUSING_TRAFFIC);
            return true;
        }, () -> "Context not closed!", 10, 100);
    }

    private void checkReadiness(ReadinessState expectedState) {
        Assertions.assertEquals(
                expectedState,
                ((RewardContextHolderServiceImpl)rewardContextHolderService).getState(null)
        );
    }
}