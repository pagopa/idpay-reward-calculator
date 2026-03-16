package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.connector.repository.secondary.OnboardingFamiliesRepository;
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
  public Mono<Void> processOnboardingOutcome(String initiativeId, String userId) {
    return rewardContextHolderService.getInitiativeConfig(initiativeId)
        .switchIfEmpty(Mono.error(new IllegalArgumentException(
            "Cannot find initiative having id %s".formatted(initiativeId))))
        .flatMap(initiativeConfig -> {
          InitiativeGeneralDTO.BeneficiaryTypeEnum bType = initiativeConfig.getBeneficiaryType();

          if (InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(bType)) {
            return onboardingFamiliesRepository.findByMemberIdsInAndInitiativeId(userId,
                    initiativeId)
                .collectSortedList(COMPARATOR_FAMILIES_CREATE_DATE_DESC)
                .flatMap(families -> {
                  if (families == null || families.isEmpty()) {
                    return Mono.empty();
                  }
                  return userInitiativeCountersRepository.createIfNotExists(
                          families.getFirst().getFamilyId(), bType, initiativeId)
                      .then();
                });
          } else {
            return userInitiativeCountersRepository.createIfNotExists(userId, bType,
                    initiativeId)
                .then();
          }
        })
        .onErrorResume(e -> {
          log.error(
              "[ONBOARDING_OUTCOME] Error while creating user initiative counter for initiative {} and user {}",
              initiativeId, userId, e);
          return Mono.empty();
        });
  }

}