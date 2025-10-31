package it.gov.pagopa.reward.service.recess;

import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.connector.repository.secondary.HpanInitiativesRepository;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@Slf4j
public class InstrumentApiServiceImpl implements InstrumentApiService {

    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final HpanInitiativesRepository hpanInitiativesRepository;

    public InstrumentApiServiceImpl(UserInitiativeCountersRepository userInitiativeCountersRepository, HpanInitiativesRepository hpanInitiativesRepository) {
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.hpanInitiativesRepository = hpanInitiativesRepository;
    }

    @Override
    public Mono<Void> disableUserInitiativeInstruments(String userId, String initiativeId) {

        return hpanInitiativesRepository.setUserInitiativeStatus(userId, initiativeId, HpanInitiativeStatus.INACTIVE)
                .map(hpanInitiative -> {
                    hpanInitiative.getOnboardedInitiatives().stream()
                            .filter(onboardedInitiative ->
                                    initiativeId.equals(onboardedInitiative.getInitiativeId()))
                            .forEach(filteredInitiative -> {
                                String entityId = filteredInitiative.getFamilyId() != null ?
                                        filteredInitiative.getFamilyId() : userId;
                                userInitiativeCountersRepository.updateEntityIdByInitiativeIdAndEntityId(
                                        initiativeId, entityId, "HISTORY_" + entityId).blockLast();
                            });
                    return hpanInitiative;
                }).then();
    }

    @Override
    public Mono<Void> enableUserInitiativeInstruments(String userId, String initiativeId) {
        return hpanInitiativesRepository.setUserInitiativeStatus(userId, initiativeId, HpanInitiativeStatus.ACTIVE)
                .map(hpanInitiative -> {
                    hpanInitiative.getOnboardedInitiatives().stream()
                            .filter(onboardedInitiative ->
                                    initiativeId.equals(onboardedInitiative.getInitiativeId()))
                            .forEach(filteredInitiative -> {
                                String entityId = filteredInitiative.getFamilyId() != null ?
                                        filteredInitiative.getFamilyId() : userId;
                                userInitiativeCountersRepository.updateEntityIdByInitiativeIdAndEntityId(
                                        initiativeId, "HISTORY_"+entityId, entityId).blockLast();
                            });
                    return hpanInitiative;
                }).then();
    }
}
