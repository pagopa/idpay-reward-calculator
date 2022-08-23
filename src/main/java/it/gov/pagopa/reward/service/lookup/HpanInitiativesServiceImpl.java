package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2EntityMapper;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class HpanInitiativesServiceImpl implements HpanInitiativesService{
    @Autowired
    private HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper;

    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;


    @Override
    public Mono<HpanInitiatives> hpanInitiativeUpdateInformation(Pair<HpanInitiativeDTO, Mono<HpanInitiatives>> hpanInitiativePair) {
        HpanInitiativeDTO hpanInitiativeDTO = hpanInitiativePair.getFirst();
        Mono<HpanInitiatives> hpanRetrieved = hpanInitiativePair.getSecond();

        return hpanRetrieved.defaultIfEmpty(hpanInitiativeDTO2EntityMapper.apply(hpanInitiativeDTO))
                .map(h-> evaluate(h,hpanInitiativeDTO));
    }
    private HpanInitiatives evaluate(HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto){
        List<String> initiativesIds;
        if(hpanInitiatives == null){
            return null;
        }else {
            initiativesIds = hpanInitiatives.getOnboardedInitiatives().stream().map(OnboardedInitiative::getInitiativeId).toList();
        }

        if (dto.getOperationType().equals(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name())){
            return addedHpanInitiative(initiativesIds, hpanInitiatives, dto);
        }else{
            return deletedHpanInitiative(initiativesIds, hpanInitiatives, dto);
        }
    }

    private HpanInitiatives addedHpanInitiative(List<String> initiativesIds, HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto){
        //add hpan non presente --> è stato popolato con uno di default visto in presedenza
        if (initiativesIds.contains(dto.getInitiativeId())) {
            List<ActiveTimeInterval> intervalsList = hpanInitiatives.getOnboardedInitiatives().stream()
                    .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId()))
                    .map(OnboardedInitiative::getActiveTimeIntervals)
                    .toList().get(0);

            if(intervalsList.stream().anyMatch(activeTimeInterval -> dto.getOperationDate().isBefore(activeTimeInterval.getStartInterval()))){
            /*TODO
            *  ignoriamo ogni aggiunta che viene effettuata in data precedente all'ultima data di attivazione
            * (1, 3)  (6, 9) (15,17)
            * Add 0 -> ignorato
            * add 4 -> ignorato*/
                return null;
            }else {
                OnboardedInitiative onboardedInitiative = hpanInitiatives.getOnboardedInitiatives().stream()
                        .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId())).toList().get(0);

                onboardedInitiative.getActiveTimeIntervals()
                        .add(ActiveTimeInterval.builder()
                                .startInterval(dto.getOperationDate())
                                .build());
                return hpanInitiatives;
            }

        } else {
            OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                    .initiativeId(dto.getInitiativeId())
                    .acceptanceDate(dto.getOperationDate().toLocalDate()) //TODO da ricontrollare
                    .activeTimeIntervals(List.of(ActiveTimeInterval.builder().startInterval(dto.getOperationDate()).build()))
                    .build();
            hpanInitiatives.getOnboardedInitiatives().add(onboardedInitiative);
            return hpanInitiatives;
        }
    }

    private HpanInitiatives deletedHpanInitiative(List<String> initiativesIds, HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto) {

        OnboardedInitiative onboardedInitiative = hpanInitiatives.getOnboardedInitiatives().stream()
                .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId())).toList().get(0);

        List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();

        LocalDateTime lastActivate = Collections.max(activeTimeIntervalsList.stream().map(ActiveTimeInterval::getStartInterval).toList());
        List<ActiveTimeInterval> lastActiveInterval = activeTimeIntervalsList.stream().filter(activeTimeInterval -> activeTimeInterval.getStartInterval().equals(lastActivate)).toList();

        if(initiativesIds.contains(dto.getInitiativeId())
                && dto.getOperationDate().isAfter(lastActivate)
                && lastActiveInterval.get(0).getEndInterval()==null){

            onboardedInitiative.getActiveTimeIntervals().remove(ActiveTimeInterval.builder()
                    .startInterval(lastActivate)
                    .build());

            onboardedInitiative.getActiveTimeIntervals().add(ActiveTimeInterval.builder()
                    .startInterval(lastActivate)
                    .endInterval(dto.getOperationDate())
                    .build());

            return hpanInitiatives;

        }
        return null;
    }

}
