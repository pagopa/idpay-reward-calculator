package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.connector.repository.secondary.OnboardingFamiliesRepository;
import it.gov.pagopa.reward.dto.EvaluationDTO;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.model.OnboardingFamilies;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OnboardingOutcomeMediatorServiceImpl implements OnboardingOutcomeMediatorService {

  public static final Comparator<OnboardingFamilies> COMPARATOR_FAMILIES_CREATE_DATE_DESC = Comparator.comparing(
      OnboardingFamilies::getCreateDate).reversed();

  private final UserInitiativeCountersRepository userInitiativeCountersRepository;
  private final RewardContextHolderService rewardContextHolderService;
  private final OnboardingFamiliesRepository onboardingFamiliesRepository;

  public OnboardingOutcomeMediatorServiceImpl(
      UserInitiativeCountersRepository userInitiativeCountersRepository,
      RewardContextHolderService rewardContextHolderService,
      OnboardingFamiliesRepository onboardingFamiliesRepository) {
    this.userInitiativeCountersRepository = userInitiativeCountersRepository;
    this.rewardContextHolderService = rewardContextHolderService;
    this.onboardingFamiliesRepository = onboardingFamiliesRepository;
  }

  @Override
  public Mono<EvaluationDTO> processOnboardingOutcome(EvaluationDTO payload) {

    // Only create counters when onboarding was successful
    if (!"ONBOARDING_OK".equals(payload.status())) {
      return Mono.just(payload);
    }

    return rewardContextHolderService.getInitiativeConfig(payload.initiativeId())
        .switchIfEmpty(Mono.error(new IllegalArgumentException(
            "Cannot find initiative having id %s".formatted(payload.initiativeId()))))
        .flatMap(initiativeConfig -> {
          InitiativeGeneralDTO.BeneficiaryTypeEnum bType = initiativeConfig.getBeneficiaryType();

          if (InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(bType)) {
            // family-level counters: prefer familyId from event if present, otherwise lookup
            if (payload.familyId() != null && !payload.familyId().isBlank()) {
              return userInitiativeCountersRepository.createIfNotExists(payload.familyId(), bType,
                      payload.initiativeId())
                  .thenReturn(payload);
            }

            return onboardingFamiliesRepository.findByMemberIdsInAndInitiativeId(payload.userId(),
                    payload.initiativeId())
                .collectSortedList(COMPARATOR_FAMILIES_CREATE_DATE_DESC)
                .flatMap(families -> {
                  if (families == null || families.isEmpty()) {
                    return Mono.just(payload);
                  }
                  return userInitiativeCountersRepository.createIfNotExists(
                          families.getFirst().getFamilyId(), bType, payload.initiativeId())
                      .thenReturn(payload);
                });
          } else {
            // person-level counters
            return userInitiativeCountersRepository.createIfNotExists(payload.userId(), bType,
                    payload.initiativeId())
                .thenReturn(payload);
          }
        })
        .onErrorResume(e -> {
          log.error(
              "[ONBOARDING_OUTCOME] Error while creating user initiative counter for event {}",
              payload, e);
          return Mono.just(payload);
        });
  }

}