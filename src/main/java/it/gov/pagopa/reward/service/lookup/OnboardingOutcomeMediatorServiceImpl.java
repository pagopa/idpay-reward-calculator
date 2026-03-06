package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.connector.repository.secondary.OnboardingFamiliesRepository;
import it.gov.pagopa.reward.dto.EvaluationDTO;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.model.OnboardingFamilies;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OnboardingOutcomeMediatorServiceImpl extends
    BaseKafkaConsumer<EvaluationDTO, EvaluationDTO> implements
    OnboardingOutcomeMediatorService {

  public static final Comparator<OnboardingFamilies> COMPARATOR_FAMILIES_CREATE_DATE_DESC = Comparator.comparing(
      OnboardingFamilies::getCreateDate).reversed();

  private final Duration commitDelay;

  private final UserInitiativeCountersRepository userInitiativeCountersRepository;
  private final RewardErrorNotifierService rewardErrorNotifierService;
  private final ObjectReader objectReader;
  private final RewardContextHolderService rewardContextHolderService;
  private final OnboardingFamiliesRepository onboardingFamiliesRepository;

  public OnboardingOutcomeMediatorServiceImpl(
      @Value("${spring.application.name}") String applicationName,
      @Value("${spring.cloud.stream.kafka.bindings.onboardingOutcome-in-0.consumer.ackTime}") Long commitMillis,
      UserInitiativeCountersRepository userInitiativeCountersRepository,
      ObjectMapper objectMapper,
      RewardErrorNotifierService rewardErrorNotifierService,
      RewardContextHolderService rewardContextHolderService,
      OnboardingFamiliesRepository onboardingFamiliesRepository) {
    super(applicationName);
    this.commitDelay = Duration.ofMillis(commitMillis);

    this.userInitiativeCountersRepository = userInitiativeCountersRepository;
    this.objectReader = objectMapper.readerFor(EvaluationDTO.class);
    this.rewardErrorNotifierService = rewardErrorNotifierService;
    this.rewardContextHolderService = rewardContextHolderService;
    this.onboardingFamiliesRepository = onboardingFamiliesRepository;
  }

  @Override
  protected Duration getCommitDelay() {
    return commitDelay;
  }

  @Override
  protected void subscribeAfterCommits(Flux<List<EvaluationDTO>> afterCommits2subscribe) {
    afterCommits2subscribe.subscribe(
        updateResult -> log.info("[ONBOARDING_OUTCOME] Processed offsets committed successfully"));
  }

  @Override
  protected void notifyError(Message<String> message, Throwable e) {
    rewardErrorNotifierService.notifyTransactionEvaluation(message,
        "[ONBOARDING_OUTCOME] An error occurred evaluating hpan update", false, e);
  }

  @Override
  protected ObjectReader getObjectReader() {
    return objectReader;
  }

  @Override
  protected Consumer<Throwable> onDeserializationError(Message<String> message) {
    return e -> rewardErrorNotifierService.notifyTransactionEvaluation(message,
        "[ONBOARDING_OUTCOME] Unexpected JSON", true, e);
  }

  @Override
  protected Mono<EvaluationDTO> execute(EvaluationDTO payload, Message<String> message,
      Map<String, Object> ctx) {
//        LocalDateTime evaluationDate = LocalDateTime.now();

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
            // TODO is this logic correct?
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
          // rewardErrorNotifierService.notifyHpanUpdateEvaluation(message, "[HPAN_INITIATIVE_OP] Error while creating counters", true, e);
          return Mono.just(payload);
        });
  }

  @Override
  public String getFlowName() {
    return "ONBOARDING_OUTCOME";
  }

}