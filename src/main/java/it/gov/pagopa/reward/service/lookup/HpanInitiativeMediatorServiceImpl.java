package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
public class HpanInitiativeMediatorServiceImpl implements HpanInitiativeMediatorService{
    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    private HpanInitiativesService hpanInitiativesService;

    @Override
    public void execute(Flux<HpanInitiativeDTO> hpanInitiativeDTOFlux) {
        hpanInitiativeDTOFlux
                .map(h -> Pair.of(h, hpanInitiativesRepository.findById(h.getHpan())));
    }
}