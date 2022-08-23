package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
    public void execute(Flux<HpanInitiativeDTO> hpanInitiativeDTOFlux) {
        hpanInitiativeDTOFlux
                .map(hpanInitiativeDTO -> hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO, hpanInitiativesRepository.findById(hpanInitiativeDTO.getHpan()))))
                .map(hpanInitiativesMono -> hpanInitiativesMono.subscribe(hpanInitiativesRepository::save));
    }
}