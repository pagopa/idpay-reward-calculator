package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
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

    public HpanInitiativeMediatorServiceImpl(HpanInitiativesRepository hpanInitiativesRepository, HpanInitiativesService hpanInitiativesService, ObjectMapper objectMapper, ErrorNotifierService errorNotifierService) {
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.hpanInitiativesService = hpanInitiativesService;
        this.objectReader = objectMapper.readerFor(HpanInitiativeDTO.class);
        this.errorNotifierService = errorNotifierService;
    }


    @Override
    public void execute(Flux<Message<String>> messageFlux) {
        messageFlux
                .flatMap(this::execute)
                .subscribe(hpanInitiativesMono ->
                        hpanInitiativesMono.flatMap(hpanInitiativesRepository::save)
                                .subscribe(hpanInitiatives -> log.debug("Updated Hpan: {}",hpanInitiatives.getHpan())));
    }

    public Mono<Mono<HpanInitiatives>> execute(Message<String> message) {
        long before = System.currentTimeMillis();
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .mapNotNull(hpanInitiativeDTO -> hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO, hpanInitiativesRepository.findById(hpanInitiativeDTO.getHpan()))))
                .onErrorResume(e -> {
                    errorNotifierService.notifyHpanUpdateEvaluation(message, "An error occurred evaluating hpan update", true, e);
                    return Mono.empty();
                })
                .doFinally(s -> log.info("[PERFORMANCE_LOG] Time for elaborate a Hpan update: {} ms", System.currentTimeMillis()-before));
    }

    private HpanInitiativeDTO deserializeMessage(Message<String> message) {
       return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyRewardRuleBuilder(message, "Unexpected JSON", true, e ));

    }
}