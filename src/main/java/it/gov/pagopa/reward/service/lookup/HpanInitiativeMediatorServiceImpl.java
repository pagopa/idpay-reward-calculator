package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2InitialEntityMapper;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
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

    private final HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper;

    private final HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper;


    public HpanInitiativeMediatorServiceImpl(HpanInitiativesRepository hpanInitiativesRepository, HpanInitiativesService hpanInitiativesService, ObjectMapper objectMapper, ErrorNotifierService errorNotifierService, HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper, HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper) {
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.hpanInitiativesService = hpanInitiativesService;
        this.objectReader = objectMapper.readerFor(HpanInitiativeBulkDTO.class);
        this.errorNotifierService = errorNotifierService;
        this.hpanInitiativeDTO2InitialEntityMapper = hpanInitiativeDTO2InitialEntityMapper;
        this.hpanUpdateBulk2SingleMapper = hpanUpdateBulk2SingleMapper;
    }

    @Override
    public void execute(Flux<Message<String>> messageFlux) {
        messageFlux
                .flatMap(this::execute,1)
                .subscribe(hpanInitiatives -> log.debug("Updated Hpan: {}",hpanInitiatives.getHpan()));
    }

   public Flux<HpanInitiatives> execute(Message<String> message) {
        long before = System.currentTimeMillis();
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMapMany(this::initializingHpanInitiativeDTO)
                .flatMap(this::retrieveAndEvaluateHpan, 1)
                .flatMap(hpanInitiativesRepository::save, 1) //push
                .onErrorResume(e ->  {errorNotifierService.notifyHpanUpdateEvaluation(message, "An error occurred evaluating hpan update", false, e);
                    return Flux.empty();})
                .doFinally(s -> log.info("[PERFORMANCE_LOG] Time for elaborate a Hpan update: {} ms", System.currentTimeMillis() - before));
    }

    private HpanInitiativeBulkDTO deserializeMessage(Message<String> message) {
       return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyHpanUpdateEvaluation(message, "Unexpected JSON", true, e));
    }

    private Mono<HpanInitiatives> retrieveAndEvaluateHpan(HpanInitiativeDTO hpanInitiativeDTO){
        return hpanInitiativesRepository.findById(hpanInitiativeDTO.getHpan())
                .switchIfEmpty( hpanInitiativesRepository.save(hpanInitiativeDTO2InitialEntityMapper.apply(hpanInitiativeDTO)))
                .mapNotNull(hpanInitiatives -> hpanInitiativesService.evaluate(hpanInitiativeDTO,hpanInitiatives));
    }

    private Flux<HpanInitiativeDTO> initializingHpanInitiativeDTO(HpanInitiativeBulkDTO dto){
        return Flux.fromIterable(dto.getHpanList().stream().map(hpan -> hpanUpdateBulk2SingleMapper.apply(dto,hpan)).toList());
    }
}