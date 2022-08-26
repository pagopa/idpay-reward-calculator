package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
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

    public HpanInitiativeMediatorServiceImpl(HpanInitiativesRepository hpanInitiativesRepository, HpanInitiativesService hpanInitiativesService) {
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.hpanInitiativesService = hpanInitiativesService;
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
                .doFinally(s -> log.debug("[PERFORMANCE_LOG] Time for elaborate a Hpan update: {} ms", System.currentTimeMillis()-before));
    }

    private HpanInitiativeDTO deserializeMessage(Message<String> message) {
            ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(message.getPayload(),HpanInitiativeDTO.class);
        } catch (JsonProcessingException e) {
            return null;
        }

    }
}