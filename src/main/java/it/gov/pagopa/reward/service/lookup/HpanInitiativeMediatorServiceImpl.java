package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateEvaluateDTO2HpanInitiativeMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.BaseKafkaConsumer;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class HpanInitiativeMediatorServiceImpl extends BaseKafkaConsumer<HpanInitiativeBulkDTO, HpanInitiativeBulkDTO> implements HpanInitiativeMediatorService {

    private final Duration commitDelay;

    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final HpanInitiativesService hpanInitiativesService;
    private final ErrorNotifierService errorNotifierService;
    private final ObjectReader objectReader;

    private final HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper;

    private final HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper;

    public HpanInitiativeMediatorServiceImpl(
            @Value("${spring.cloud.stream.kafka.bindings.hpanInitiativeConsumer-in-0.consumer.ackTime}") Long commitMillis,
            HpanInitiativesRepository hpanInitiativesRepository,
            HpanInitiativesService hpanInitiativesService,
            ObjectMapper objectMapper,
            ErrorNotifierService errorNotifierService,
            HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper,
            HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper) {
        this.commitDelay = Duration.ofMillis(commitMillis);

        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.hpanInitiativesService = hpanInitiativesService;
        this.objectReader = objectMapper.readerFor(HpanInitiativeBulkDTO.class);
        this.errorNotifierService = errorNotifierService;
        this.hpanUpdateEvaluateDTO2HpanInitiativeMapper = hpanUpdateEvaluateDTO2HpanInitiativeMapper;
        this.hpanUpdateBulk2SingleMapper = hpanUpdateBulk2SingleMapper;
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<HpanInitiativeBulkDTO>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(updateResult -> log.debug("[HPAN_INITIATIVE_OP] A change has occurred"));
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyHpanUpdateEvaluation(message, "[HPAN_INITIATIVE_OP] An error occurred evaluating hpan update", false, e);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyHpanUpdateEvaluation(message, "[HPAN_INITIATIVE_OP] Unexpected JSON", true, e);
    }

    @Override
    protected Mono<HpanInitiativeBulkDTO> execute(HpanInitiativeBulkDTO payload, Message<String> message, Map<String, Object> ctx) {
        return Mono.just(payload)
                .flatMapMany(this::evaluate)
                .collectList()

                .then(Mono.just(payload));
    }

    @Override
    protected String getFlowName() {
        return "HPAN_INITIATIVE_OP";
    }

    private Flux<UpdateResult> evaluate(HpanInitiativeBulkDTO hpanInitiativeBulkDTO) {
        return initializingHpanInitiativeDTO(hpanInitiativeBulkDTO)
                .flatMap(this::findAndModify);
    }

    private Flux<HpanUpdateEvaluateDTO> initializingHpanInitiativeDTO(HpanInitiativeBulkDTO dto) {
        return Flux.fromIterable(dto.getHpanList().stream().map(hpan -> hpanUpdateBulk2SingleMapper.apply(dto, hpan)).toList());
    }

    private Mono<UpdateResult> findAndModify(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        return hpanInitiativesRepository.findById(hpanUpdateEvaluateDTO.getHpan())
                .switchIfEmpty(Mono.defer(() -> getNewHpanInitiatives(hpanUpdateEvaluateDTO)))
                .mapNotNull(hpanInitiatives -> hpanInitiativesService.evaluate(hpanUpdateEvaluateDTO, hpanInitiatives))
                .flatMap(oi -> hpanInitiativesRepository.setInitiative(hpanUpdateEvaluateDTO.getHpan(), oi));
    }

    private Mono<HpanInitiatives> getNewHpanInitiatives(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        if (hpanUpdateEvaluateDTO.getOperationType().equals(HpanInitiativeConstants.ADD_INSTRUMENT)) {
            HpanInitiatives createHpanInitiatives = hpanUpdateEvaluateDTO2HpanInitiativeMapper.apply(hpanUpdateEvaluateDTO);
            return hpanInitiativesRepository
                    .createIfNotExist(createHpanInitiatives)
                    .then(Mono.just(createHpanInitiatives));
        } else {
            log.error("Unexpected use case, the hpan is not present into DB. Source message: {}", hpanUpdateEvaluateDTO);
            return Mono.empty();
        }
    }
}