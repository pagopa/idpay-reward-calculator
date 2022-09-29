package it.gov.pagopa.reward.service.build;


import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class RewardRuleMediatorServiceImplTest {

    @Mock
    private RewardRule2DroolsRuleService rewardRule2DroolsRuleService;

    @Mock
    private DroolsRuleRepository droolsRuleRepository;

    @Mock
    private KieContainerBuilderService kieContainerBuilderService;

    @Mock
    private RewardContextHolderService rewardContextHolderService;

    @Mock
    private ErrorNotifierService errorNotifierService;

    private final KieContainer newKieContainerBuilt = Mockito.mock(KieContainer.class);

    private RewardRuleMediatorService rewardRuleMediatorService;

    @BeforeEach
    void setUp() {
        rewardRuleMediatorService = new RewardRuleMediatorServiceImpl(1000L,
                "PT1S",
                rewardRule2DroolsRuleService,
                droolsRuleRepository,
                kieContainerBuilderService,
                rewardContextHolderService,
                errorNotifierService,
                TestUtils.objectMapper);

        Mockito.when(rewardRule2DroolsRuleService.apply(Mockito.any())).thenAnswer(invocation -> {
            InitiativeReward2BuildDTO i = invocation.getArgument(0);
            return new DroolsRule(i.getInitiativeId(), i.getInitiativeName(),"RULE",
                    InitiativeConfig.builder()
                            .initiativeId(i.getInitiativeId())
                            .beneficiaryBudget(new BigDecimal("1000.00"))
                            .endDate(LocalDate.now())
                            .dailyThreshold(true)
                            .weeklyThreshold(true)
                            .monthlyThreshold(false)
                            .yearlyThreshold(false)
                            .build());
        });

        Mockito.when(droolsRuleRepository.save(Mockito.any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        Mockito.when(kieContainerBuilderService.buildAll()).thenReturn(Mono.just(newKieContainerBuilt));
    }

    @Test
    void testSuccessful(){
        // given
        int N = 10;
        List<InitiativeReward2BuildDTO> initiatives = IntStream.range(0, N).mapToObj(InitiativeReward2BuildDTOFaker::mockInstance).collect(Collectors.toList());
        Flux<Message<String>> inputFlux = Flux.fromIterable(initiatives).map(TestUtils::jsonSerializer).map(MessageBuilder::withPayload).map(MessageBuilder::build);

        // when
        rewardRuleMediatorService.execute(inputFlux);

        // then
        Mockito.verify(rewardRule2DroolsRuleService, Mockito.times(N)).apply(Mockito.any());
        initiatives.forEach(i -> {
            Mockito.verify(rewardRule2DroolsRuleService).apply(i);
            Mockito.verify(droolsRuleRepository).save(Mockito.argThat(dr -> dr.getId().equals(i.getInitiativeId())));
        });

        Mockito.verify(kieContainerBuilderService, Mockito.atLeast(1)).buildAll();
        Mockito.verify(rewardContextHolderService, Mockito.atLeast(1)).setRewardRulesKieContainer(Mockito.same(newKieContainerBuilt));
        Mockito.verifyNoInteractions(errorNotifierService);
    }


}