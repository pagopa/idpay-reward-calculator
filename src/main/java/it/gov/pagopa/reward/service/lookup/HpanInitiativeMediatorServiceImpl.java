package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.HpanUpdateOutcomeDTO;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanList2HpanUpdateOutcomeDTOMapper;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanUpdateEvaluateDTO2HpanInitiativeMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.OnboardingFamilies;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class HpanInitiativeMediatorServiceImpl extends BaseKafkaConsumer<HpanInitiativeBulkDTO, HpanInitiativeBulkDTO> implements HpanInitiativeMediatorService {

    public static final Comparator<OnboardingFamilies> COMPARATOR_FAMILIES_CREATE_DATE_DESC = Comparator.comparing(OnboardingFamilies::getCreateDate).reversed();

    private final Duration commitDelay;

    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final HpanInitiativesService hpanInitiativesService;
    private final RewardErrorNotifierService rewardErrorNotifierService;
    private final HpanUpdateNotifierService hpanUpdateNotifierService;
    private final ObjectReader objectReader;

    private final HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper;
    private final HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper;
    private final HpanList2HpanUpdateOutcomeDTOMapper hpanList2HpanUpdateOutcomeDTOMapper;
    private final RewardContextHolderService rewardContextHolderService;
    private final OnboardingFamiliesRepository onboardingFamiliesRepository;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public HpanInitiativeMediatorServiceImpl(
            @Value("${spring.application.name}") String applicationName,
            @Value("${spring.cloud.stream.kafka.bindings.hpanInitiativeConsumer-in-0.consumer.ackTime}") Long commitMillis,
            HpanInitiativesRepository hpanInitiativesRepository,
            UserInitiativeCountersRepository userInitiativeCountersRepository,
            HpanInitiativesService hpanInitiativesService,
            HpanUpdateNotifierService hpanUpdateNotifierService,
            ObjectMapper objectMapper,
            RewardErrorNotifierService rewardErrorNotifierService,
            HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper,
            HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper,
            HpanList2HpanUpdateOutcomeDTOMapper hpanList2HpanUpdateOutcomeDTOMapper, RewardContextHolderService rewardContextHolderService, OnboardingFamiliesRepository onboardingFamiliesRepository) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);

        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.hpanInitiativesService = hpanInitiativesService;
        this.hpanUpdateNotifierService = hpanUpdateNotifierService;
        this.objectReader = objectMapper.readerFor(HpanInitiativeBulkDTO.class);
        this.rewardErrorNotifierService = rewardErrorNotifierService;
        this.hpanUpdateEvaluateDTO2HpanInitiativeMapper = hpanUpdateEvaluateDTO2HpanInitiativeMapper;
        this.hpanUpdateBulk2SingleMapper = hpanUpdateBulk2SingleMapper;
        this.hpanList2HpanUpdateOutcomeDTOMapper = hpanList2HpanUpdateOutcomeDTOMapper;
        this.rewardContextHolderService = rewardContextHolderService;
        this.onboardingFamiliesRepository = onboardingFamiliesRepository;
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<HpanInitiativeBulkDTO>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(updateResult -> log.info("[HPAN_INITIATIVE_OP] Processed offsets committed successfully"));
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        rewardErrorNotifierService.notifyHpanUpdateEvaluation(message, "[HPAN_INITIATIVE_OP] An error occurred evaluating hpan update", false, e);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> rewardErrorNotifierService.notifyHpanUpdateEvaluation(message, "[HPAN_INITIATIVE_OP] Unexpected JSON", true, e);
    }

    @Override
    protected Mono<HpanInitiativeBulkDTO> execute(HpanInitiativeBulkDTO payload, Message<String> message, Map<String, Object> ctx) {
        LocalDateTime evaluationDate = LocalDateTime.now();
        return Mono.just(payload)
                .flatMapMany(bulk -> this.evaluate(bulk,evaluationDate))
                .collectList()
                .doOnNext(hpanList -> {
                    if(!payload.getChannel().equals(HpanInitiativeConstants.CHANEL_PAYMENT_MANAGER) &&
                        !payload.getChannel().equals(HpanInitiativeConstants.CHANNEL_IDPAY_PAYMENT)){
                        HpanUpdateOutcomeDTO outcome = hpanList2HpanUpdateOutcomeDTOMapper.apply(hpanList, payload, evaluationDate);
                        try {
                            if (!hpanUpdateNotifierService.notify(outcome)) {
                                throw new IllegalStateException("[HPAN_INITIATIVE_OUTCOME] Something gone wrong while hpan update notify");
                            }
                        } catch (Exception e) {
                            log.error("[UNEXPECTED_HPAN_INITIATIVE_OUTCOME] Unexpected error occurred publishing rewarded transaction: {}", outcome);
                            rewardErrorNotifierService.notifyHpanUpdateOutcome(HpanUpdateNotifierServiceImpl.buildMessage(outcome), "[HPAN_UPDATE_OUTCOME] An error occurred while publishing the hpan update outcome", true, e);
                        }
                    }
                })


                .then(Mono.just(payload));
    }

    @Override
    public String getFlowName() {
        return "HPAN_INITIATIVE_OP";
    }

    private Flux<String> evaluate(HpanInitiativeBulkDTO hpanInitiativeBulkDTO, LocalDateTime evaluationDate) {
        return initializingHpanInitiativeDTO(hpanInitiativeBulkDTO, evaluationDate)
                .flatMap(this::findAndModify);
    }

    private Flux<HpanUpdateEvaluateDTO> initializingHpanInitiativeDTO(HpanInitiativeBulkDTO dto, LocalDateTime evaluationDate) {
        return Flux.fromIterable(dto.getInfoList().stream().map(infoHpan -> hpanUpdateBulk2SingleMapper.apply(dto, infoHpan, evaluationDate)).toList());
    }

    private Mono<String> findAndModify(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {

        return hpanInitiativesRepository.findById(hpanUpdateEvaluateDTO.getHpan())
                .switchIfEmpty(Mono.defer(() -> getNewHpanInitiatives(hpanUpdateEvaluateDTO)))
                .mapNotNull(hpanInitiatives -> hpanInitiativesService.evaluate(hpanUpdateEvaluateDTO, hpanInitiatives))
                .flatMap(oi -> initializeCounterAndRetrieveFamily(hpanUpdateEvaluateDTO, oi))
                .flatMap(oi -> hpanInitiativesRepository.setInitiative(hpanUpdateEvaluateDTO.getHpan(), oi))
                .map(ur -> hpanUpdateEvaluateDTO.getHpan());
    }

    @NotNull
    private Mono<OnboardedInitiative> initializeCounterAndRetrieveFamily(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO, OnboardedInitiative oi) {
        if(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT.equals(hpanUpdateEvaluateDTO.getOperationType()) && oi.getActiveTimeIntervals().size() == 1){
            return retrieveAndEvaluateInitiative(hpanUpdateEvaluateDTO)
                    .doOnNext(oi::setFamilyId)
                    .switchIfEmpty(Mono.just(hpanUpdateEvaluateDTO.getUserId()))
                    .flatMap(counterSubjectId  -> userInitiativeCountersRepository.createIfNotExists(counterSubjectId , hpanUpdateEvaluateDTO.getInitiativeId()))
                    .then(Mono.just(oi));
        }
        return Mono.just(oi);
    }

    private Mono<HpanInitiatives> getNewHpanInitiatives(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        if (hpanUpdateEvaluateDTO.getOperationType().equals(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)) {
            HpanInitiatives createHpanInitiatives = hpanUpdateEvaluateDTO2HpanInitiativeMapper.apply(hpanUpdateEvaluateDTO);
            return hpanInitiativesRepository
                    .createIfNotExist(createHpanInitiatives)
                    .then(Mono.just(createHpanInitiatives));
        } else {
            log.error("Unexpected use case, the hpan is not present into DB. Source message: {}", hpanUpdateEvaluateDTO);
            return Mono.empty();
        }
    }

    private Mono<String> retrieveAndEvaluateInitiative(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        return rewardContextHolderService.getInitiativeConfig(hpanUpdateEvaluateDTO.getInitiativeId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Cannot find initiative having id %s".formatted(hpanUpdateEvaluateDTO.getInitiativeId()))))
                .flatMap(i -> InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(i.getBeneficiaryType())
                        ? retrieveFamilyId(hpanUpdateEvaluateDTO)
                        : Mono.empty());
    }

    private Mono<String> retrieveFamilyId(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO){
        return onboardingFamiliesRepository.findByMemberIdsInAndInitiativeId(hpanUpdateEvaluateDTO.getUserId(), hpanUpdateEvaluateDTO.getInitiativeId())
                .collectSortedList(COMPARATOR_FAMILIES_CREATE_DATE_DESC)
                .map(f -> f.get(0).getFamilyId());
    }
}