package it.gov.pagopa.reward.service.lookup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.mongodb.client.result.UpdateResult;

import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.connector.repository.secondary.OnboardingFamiliesRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.model.OnboardingFamilies;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OnboardingOutcomeMediatorServiceImplTest {

  @Mock private UserInitiativeCountersRepository countersRepository;
  @Mock private RewardContextHolderService rewardContextHolderService;
  @Mock private OnboardingFamiliesRepository onboardingFamiliesRepository;

  private OnboardingOutcomeMediatorServiceImpl sut;

  @BeforeEach
  void setUp() {
    sut = new OnboardingOutcomeMediatorServiceImpl(
        countersRepository,
        rewardContextHolderService,
        onboardingFamiliesRepository);
  }

  @Test
  void shouldCreatePersonCounterWhenPF() {
    InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF).build();

    when(rewardContextHolderService.getInitiativeConfig("I2")).thenReturn(Mono.just(cfg));
    when(countersRepository.createIfNotExists("U2", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, "I2"))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 0L, null)));

    StepVerifier.create(sut.processOnboardingOutcome("I2", "U2"))
        .verifyComplete();

    verify(countersRepository).createIfNotExists("U2", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, "I2");
  }

  @Test
  void shouldCreateFamilyCounterWhenNFWithoutFamilyIdLookup() {
    InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF).build();

    OnboardingFamilies older = OnboardingFamilies.builder().familyId("FAM_OLD").createDate(Instant.now().minus(2, ChronoUnit.DAYS)).build();
    OnboardingFamilies latest = OnboardingFamilies.builder().familyId("FAM_NEW").createDate(Instant.now()).build();

    when(rewardContextHolderService.getInitiativeConfig("I4")).thenReturn(Mono.just(cfg));
    when(onboardingFamiliesRepository.findByMemberIdsInAndInitiativeId("U4", "I4"))
        .thenReturn(Flux.just(older, latest));
    when(countersRepository.createIfNotExists("FAM_NEW", InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, "I4"))
        .thenReturn(Mono.just(UpdateResult.acknowledged(1, 0L, null)));

    StepVerifier.create(sut.processOnboardingOutcome("I4", "U4"))
        .verifyComplete();

    verify(countersRepository).createIfNotExists("FAM_NEW", InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, "I4");
  }

  @Test
  void shouldNotCreateCounterOnMissingInitiativeConfig() {
    when(rewardContextHolderService.getInitiativeConfig("I5")).thenReturn(Mono.empty());

    StepVerifier.create(sut.processOnboardingOutcome("I5", "U5"))
        .verifyComplete();

    verify(countersRepository, never()).createIfNotExists(any(), any(), any());
  }

  @Test
  void shouldNotCreateFamilyCounterWhenNFLookupEmpty() {
    InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF).build();

    when(rewardContextHolderService.getInitiativeConfig("I6")).thenReturn(Mono.just(cfg));
    when(onboardingFamiliesRepository.findByMemberIdsInAndInitiativeId("U6", "I6"))
        .thenReturn(Flux.empty());

    StepVerifier.create(sut.processOnboardingOutcome("I6", "U6"))
        .verifyComplete();

    verify(countersRepository, never()).createIfNotExists(any(), any(), any());
  }

  @Test
  void shouldCompleteOnErrorWhenCreateFails() {
    InitiativeConfig cfg = InitiativeConfig.builder().beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF).build();

    when(rewardContextHolderService.getInitiativeConfig("I7")).thenReturn(Mono.just(cfg));
    when(countersRepository.createIfNotExists("U7", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, "I7"))
        .thenReturn(Mono.error(new RuntimeException("boom")));

    StepVerifier.create(sut.processOnboardingOutcome("I7", "U7"))
        .verifyComplete();
  }
}
