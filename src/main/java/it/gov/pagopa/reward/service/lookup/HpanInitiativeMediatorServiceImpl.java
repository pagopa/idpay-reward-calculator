package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateEvaluateDTO2HpanInitiativeMapper;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class HpanInitiativeMediatorServiceImpl implements HpanInitiativeMediatorService{
    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final HpanInitiativesService hpanInitiativesService;
    private final ErrorNotifierService errorNotifierService;
    private final ObjectReader objectReader;

    private final HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper;

    private final HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper;


    public HpanInitiativeMediatorServiceImpl(HpanInitiativesRepository hpanInitiativesRepository, HpanInitiativesService hpanInitiativesService, ObjectMapper objectMapper, ErrorNotifierService errorNotifierService, HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper, HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper) {
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.hpanInitiativesService = hpanInitiativesService;
        this.objectReader = objectMapper.readerFor(HpanInitiativeBulkDTO.class);
        this.errorNotifierService = errorNotifierService;
        this.hpanUpdateEvaluateDTO2HpanInitiativeMapper = hpanUpdateEvaluateDTO2HpanInitiativeMapper;
        this.hpanUpdateBulk2SingleMapper = hpanUpdateBulk2SingleMapper;
    }

    @Override
    public void execute(Flux<Message<String>> messageFlux) {
        messageFlux
                .flatMap(this::execute,1)
                .subscribe(updateResult -> log.debug("A change has occurred"));
    }
    public Flux<UpdateResult> execute(Message<String> message) {
        long before = System.currentTimeMillis();
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMapMany(this::evaluate)
                .onErrorResume(e ->  {errorNotifierService.notifyHpanUpdateEvaluation(message, "An error occurred evaluating hpan update", false, e);
                    return Flux.empty();})
                .doFinally(s -> log.info("[PERFORMANCE_LOG] Time for elaborate a Hpan update: {} ms", System.currentTimeMillis() - before));
    }

    private HpanInitiativeBulkDTO deserializeMessage(Message<String> message) {
       return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyHpanUpdateEvaluation(message, "Unexpected JSON", true, e));
    }

    private Flux<UpdateResult> evaluate(HpanInitiativeBulkDTO hpanInitiativeBulkDTO){
        return initializingHpanInitiativeDTO(hpanInitiativeBulkDTO)
                .flatMap(this::findAndModify);
    }

    private Flux<HpanUpdateEvaluateDTO> initializingHpanInitiativeDTO(HpanInitiativeBulkDTO dto){
        return Flux.fromIterable(dto.getHpanList().stream().map(hpan -> hpanUpdateBulk2SingleMapper.apply(dto,hpan)).toList());
    }

    private Mono<UpdateResult> findAndModify(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO){
        return hpanInitiativesRepository.findById(hpanUpdateEvaluateDTO.getHpan())
                .switchIfEmpty(Mono.defer(() -> getNewHpanInitiatives(hpanUpdateEvaluateDTO)))
                .mapNotNull(hpanInitiatives -> hpanInitiativesService.evaluate(hpanUpdateEvaluateDTO,hpanInitiatives))
                .flatMap(oi -> hpanInitiativesRepository.setInitiative(hpanUpdateEvaluateDTO.getHpan(), oi));
    }

    private Mono<HpanInitiatives> getNewHpanInitiatives(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        if(hpanUpdateEvaluateDTO.getOperationType().equals(HpanInitiativeConstants.ADD_INSTRUMENT)) {
            HpanInitiatives createHpanInitiatives = hpanUpdateEvaluateDTO2HpanInitiativeMapper.apply(hpanUpdateEvaluateDTO);
            return hpanInitiativesRepository
                    .createIfNotExist(createHpanInitiatives)
                    .then(Mono.just(createHpanInitiatives));
        }else {
            log.error("Unexpected use case, the hpan is not present into DB. Source message: {}", hpanUpdateEvaluateDTO);
            return Mono.empty();
        }
    }
}