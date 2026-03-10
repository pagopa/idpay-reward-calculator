package it.gov.pagopa.reward.service.lookup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.support.MessageBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;

import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.connector.repository.secondary.OnboardingFamiliesRepository;
import it.gov.pagopa.reward.dto.EvaluationDTO;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.model.OnboardingFamilies;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OnboardingOutcomeMediatorServiceImplTest {

  @Mock private UserInitiativeCountersRepository countersRepository;
  @Mock private RewardErrorNotifierService errorNotifierService;
  @Mock private RewardContextHolderService rewardContextHolderService;
  @Mock private OnboardingFamiliesRepository onboardingFamiliesRepository;

  private OnboardingOutcomeMediatorServiceImpl sut;

  @BeforeEach
  void setUp() {
    sut = new OnboardingOutcomeMediatorServiceImpl(
        "app",
        1000L,
        countersRepository,
        new ObjectMapper(),
        errorNotifierService,
        rewardContextHolderService,
        onboardingFamiliesRepository);
  }

  private EvaluationDTO buildPayload(String userId, String familyId, String initiativeId, String status) {
    return new EvaluationDTO(
        userId,
        familyId,
        initiativeId,
        "name",
        LocalDate.now().plusDays(1),
        "org",
        status,
        LocalDateTime.now(),
        null,
        List.of(),
        100L,
        "REWARD",
        "orgName",
        true,
        10L,
        "service",
        "CHANNEL",
        "mail@test",
        "name",
        "surname");
  }

  @Test
  void shouldNotCreateCounterWhenStatusNotOk() {
    EvaluationDTO payload = buildPayload("U1", null, "I1", "ONBOARDING_KO");

    StepVerifier.create(sut.execute(payload, MessageBuilder.withPayload("p").build(), java.util.Collections.emptyMap()))
        .expectNext(payload)
        .verifyComplete();

    verify(countersRepository, never()).createIfNotExists(any(), any(), any());
  }

  @Test
  void shouldCreatePersonCounterWhenPF() {
    EvaluationDTO payload = buildPayload("U2", null, "I2", "ONBOARDING_OK");
  InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF).build();

  when(rewardContextHolderService.getInitiativeConfig("I2")).thenReturn(Mono.just(cfg));
    when(countersRepository.createIfNotExists("U2", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, "I2"))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 0L, null)));

    StepVerifier.create(sut.execute(payload, MessageBuilder.withPayload("p").build(), java.util.Collections.emptyMap()))
        .expectNext(payload)
        .verifyComplete();

    verify(countersRepository).createIfNotExists("U2", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, "I2");
  }

  @Test
  void shouldCreateFamilyCounterWhenNFWithFamilyId() {
    EvaluationDTO payload = buildPayload("U3", "FAM_1", "I3", "ONBOARDING_OK");
  InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF).build();

  when(rewardContextHolderService.getInitiativeConfig("I3")).thenReturn(Mono.just(cfg));
    when(countersRepository.createIfNotExists("FAM_1", InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, "I3"))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 0L, null)));

    StepVerifier.create(sut.execute(payload, MessageBuilder.withPayload("p").build(), java.util.Collections.emptyMap()))
        .expectNext(payload)
        .verifyComplete();

    verify(countersRepository).createIfNotExists("FAM_1", InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, "I3");
  }

  @Test
  void shouldCreateFamilyCounterWhenNFWithoutFamilyIdLookup() {
    EvaluationDTO payload = buildPayload("U4", null, "I4", "ONBOARDING_OK");
  InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF).build();

    OnboardingFamilies older = OnboardingFamilies.builder().familyId("FAM_OLD").createDate(LocalDateTime.now().minusDays(2)).build();
    OnboardingFamilies latest = OnboardingFamilies.builder().familyId("FAM_NEW").createDate(LocalDateTime.now()).build();

    when(rewardContextHolderService.getInitiativeConfig("I4")).thenReturn(Mono.just(cfg));
    when(onboardingFamiliesRepository.findByMemberIdsInAndInitiativeId("U4", "I4"))
        .thenReturn(Flux.just(older, latest));
    when(countersRepository.createIfNotExists("FAM_NEW", InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, "I4"))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 0L, null)));

    StepVerifier.create(sut.execute(payload, MessageBuilder.withPayload("p").build(), java.util.Collections.emptyMap()))
        .expectNext(payload)
        .verifyComplete();

    verify(countersRepository).createIfNotExists("FAM_NEW", InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, "I4");
  }

  @Test
  void shouldReturnPayloadOnMissingInitiativeConfig() {
    EvaluationDTO payload = buildPayload("U5", null, "I5", "ONBOARDING_OK");
  when(rewardContextHolderService.getInitiativeConfig("I5")).thenReturn(Mono.empty());

  StepVerifier.create(sut.execute(payload, MessageBuilder.withPayload("p").build(), java.util.Collections.emptyMap()))
        .expectNext(payload)
        .verifyComplete();

    verify(countersRepository, never()).createIfNotExists(any(), any(), any());
  }

  @Test
  void shouldNotCreateFamilyCounterWhenNFLookupEmpty() {
    EvaluationDTO payload = buildPayload("U6", null, "I6", "ONBOARDING_OK");
    InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF).build();

    when(rewardContextHolderService.getInitiativeConfig("I6")).thenReturn(Mono.just(cfg));
    when(onboardingFamiliesRepository.findByMemberIdsInAndInitiativeId("U6", "I6"))
        .thenReturn(Flux.empty());

    StepVerifier.create(sut.execute(payload, MessageBuilder.withPayload("p").build(), java.util.Collections.emptyMap()))
        .expectNext(payload)
        .verifyComplete();

    verify(countersRepository, never()).createIfNotExists(any(), any(), any());
  }

  @Test
  void shouldNotifyOnErrorWhenCreateFails() {
    EvaluationDTO payload = buildPayload("U7", null, "I7", "ONBOARDING_OK");
    InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF).build();

    when(rewardContextHolderService.getInitiativeConfig("I7")).thenReturn(Mono.just(cfg));
    when(countersRepository.createIfNotExists("U7", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, "I7"))
        .thenReturn(Mono.error(new RuntimeException("boom")));

    StepVerifier.create(sut.execute(payload, MessageBuilder.withPayload("p").build(), java.util.Collections.emptyMap()))
        .expectNext(payload)
        .verifyComplete();

    verify(errorNotifierService).notifyOnboardingOutcome(any(), anyString(), anyBoolean(), any());
  }
}
