package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.repository.secondary.DroolsRuleRepository;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.api.KieBase;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class RewardRuleMediatorServiceImplTest {

    @Mock
    private RewardRule2DroolsRuleService rewardRule2DroolsRuleServiceMock;

    @Mock
    private DroolsRuleRepository droolsRuleRepositoryMock;

    @Mock
    private KieContainerBuilderService kieContainerBuilderServiceMock;

    @Mock
    private RewardContextHolderService rewardContextHolderServiceMock;

    @Mock
    private RewardErrorNotifierService rewardErrorNotifierServiceMock;

    private final KieBase newKieBaseBuiltMock = Mockito.mock(KieBase.class);

    void testSetUp() {
        Mockito.when(rewardRule2DroolsRuleServiceMock.apply(Mockito.any())).thenAnswer(invocation -> {
            InitiativeReward2BuildDTO i = invocation.getArgument(0);
            return new DroolsRule(i.getInitiativeId(), i.getInitiativeName(),"RULE","RULEVERSION",
                    InitiativeConfig.builder()
                            .initiativeId(i.getInitiativeId())
                            .beneficiaryBudgetCents(1000_00L)
                            .endDate(LocalDate.now())
                            .dailyThreshold(true)
                            .weeklyThreshold(true)
                            .monthlyThreshold(false)
                            .yearlyThreshold(false)
                            .build(),
                    LocalDateTime.now());
        });
        Mockito.when(droolsRuleRepositoryMock.save(Mockito.any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        Mockito.when(kieContainerBuilderServiceMock.buildAll()).thenReturn(Mono.just(newKieBaseBuiltMock));
    }

    @ParameterizedTest
    @ValueSource(longs = {800,1000,1010})
    void executeSuccessful(long commitDelay){
        // Given
        int N = 10;
        List<InitiativeReward2BuildDTO> initiatives = IntStream.range(0, N).mapToObj(InitiativeReward2BuildDTOFaker::mockInstance).collect(Collectors.toList());
        Flux<Message<String>> inputFlux = Flux.fromIterable(initiatives)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        RewardRuleMediatorService rewardRuleMediatorService = new RewardRuleMediatorServiceImpl(
                "appName",
                commitDelay,
                "PT1S",
                rewardRule2DroolsRuleServiceMock,
                droolsRuleRepositoryMock,
                kieContainerBuilderServiceMock,
                rewardContextHolderServiceMock,
                rewardErrorNotifierServiceMock,
                TestUtils.objectMapper);

        testSetUp();

        // When
        rewardRuleMediatorService.execute(inputFlux);

        // Then
        Mockito.verify(rewardRule2DroolsRuleServiceMock, Mockito.times(N)).apply(Mockito.any());
        initiatives.forEach(i -> {
            Mockito.verify(rewardRule2DroolsRuleServiceMock).apply(i);
            Mockito.verify(droolsRuleRepositoryMock).save(Mockito.argThat(dr -> dr.getId().equals(i.getInitiativeId())));
        });

        Mockito.verify(kieContainerBuilderServiceMock, Mockito.atLeast(1)).buildAll();
        Mockito.verify(rewardContextHolderServiceMock, Mockito.atLeast(1)).setRewardRulesKieBase(Mockito.same(newKieBaseBuiltMock));
        Mockito.verifyNoInteractions(rewardErrorNotifierServiceMock);
    }

    @Test
    @Disabled("TODO - Check to be updated after inactive removal as an error status")
    void otherApplicationRetryTest(){
        // Given
        RewardRuleMediatorService rewardRuleMediatorService = new RewardRuleMediatorServiceImpl(
                "appName",
                1000L,
                "PT1S",
                rewardRule2DroolsRuleServiceMock,
                droolsRuleRepositoryMock,
                kieContainerBuilderServiceMock,
                rewardContextHolderServiceMock,
                rewardErrorNotifierServiceMock,
                TestUtils.objectMapper);

        Mockito.when(kieContainerBuilderServiceMock.buildAll()).thenReturn(Mono.just(newKieBaseBuiltMock));

        HpanInitiativeBulkDTO initiative1 = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(1)
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now()).build();
        HpanInitiativeBulkDTO initiative2 = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(2)
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now()).build();


        Flux<Message<String>> msgs = Flux.just(initiative1, initiative2)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .doOnNext(m->m.setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "otherAppName".getBytes(StandardCharsets.UTF_8)))
                .map(MessageBuilder::build);

        // When
        rewardRuleMediatorService.execute(msgs);

        // Then
        Mockito.verify(kieContainerBuilderServiceMock,Mockito.atLeast(1)).buildAll();
        Mockito.verify(rewardContextHolderServiceMock, Mockito.atLeast(1)).setRewardRulesKieBase(newKieBaseBuiltMock);

        Mockito.verifyNoInteractions(rewardRule2DroolsRuleServiceMock, droolsRuleRepositoryMock, rewardErrorNotifierServiceMock);
    }

    @Test
    void executeError(){
        // Given
        InitiativeReward2BuildDTO initiativeReward2BuildDTO = InitiativeReward2BuildDTOFaker.mockInstance(1);
        Flux<Message<String>> inputFlux = Flux.just(initiativeReward2BuildDTO)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        RewardRuleMediatorService rewardRuleMediatorService = new RewardRuleMediatorServiceImpl(
                "appName",
                1000L,
                "PT1S",
                rewardRule2DroolsRuleServiceMock,
                droolsRuleRepositoryMock,
                kieContainerBuilderServiceMock,
                rewardContextHolderServiceMock,
                rewardErrorNotifierServiceMock,
                TestUtils.objectMapper);

        Mockito.when(rewardRule2DroolsRuleServiceMock.apply(Mockito.any())).thenThrow(new RuntimeException("DUMMY_EXCEPTION"));
        Mockito.when(kieContainerBuilderServiceMock.buildAll()).thenReturn(Mono.just(newKieBaseBuiltMock));

        // When
        rewardRuleMediatorService.execute(inputFlux);

        // Then
        Mockito.verify(rewardRule2DroolsRuleServiceMock, Mockito.only()).apply(Mockito.any());
        Mockito.verify(droolsRuleRepositoryMock, Mockito.never()).save(Mockito.any());

        Mockito.verify(kieContainerBuilderServiceMock, Mockito.atLeast(1)).buildAll();
        Mockito.verify(rewardContextHolderServiceMock, Mockito.atLeast(1)).setRewardRulesKieBase(Mockito.same(newKieBaseBuiltMock));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.only()).notifyRewardRuleBuilder(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());
    }
}