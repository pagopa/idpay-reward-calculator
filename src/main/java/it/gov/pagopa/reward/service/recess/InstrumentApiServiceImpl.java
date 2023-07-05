package it.gov.pagopa.reward.service.recess;

import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class InstrumentApiServiceImpl implements InstrumentApiService {

    private final HpanInitiativesRepository hpanInitiativesRepository;

    public InstrumentApiServiceImpl(HpanInitiativesRepository hpanInitiativesRepository) {
        this.hpanInitiativesRepository = hpanInitiativesRepository;
    }

    @Override
    public Mono<Void> cancelInstruments(String userId, String initiativeId) {

        return hpanInitiativesRepository.setStatus(userId, initiativeId, HpanInitiativeStatus.INACTIVE)
                .then();
    }

    @Override
    public Mono<Void> reactivateInstruments(String userId, String initiativeId) {
        return hpanInitiativesRepository.setStatus(userId, initiativeId, HpanInitiativeStatus.ACTIVE)
                .then();
    }
}
