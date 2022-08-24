package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2EntityMapper;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class HpanInitiativesServiceImpl implements HpanInitiativesService{

    private final HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper;

    public HpanInitiativesServiceImpl(HpanInitiativeDTO2EntityMapper hpanInitiativeDTO2EntityMapper) {
        this.hpanInitiativeDTO2EntityMapper = hpanInitiativeDTO2EntityMapper;
    }

    @Override
    public Mono<HpanInitiatives> hpanInitiativeUpdateInformation(Pair<HpanInitiativeDTO, Mono<HpanInitiatives>> hpanInitiativePair) {
        HpanInitiativeDTO hpanInitiativeDTO = hpanInitiativePair.getFirst();
        Mono<HpanInitiatives> hpanRetrieved = hpanInitiativePair.getSecond();

        return hpanRetrieved
               .mapNotNull(h-> evaluate(h,hpanInitiativeDTO));
    }

    private HpanInitiatives evaluate(HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto){
        List<String> initiativesIds = hpanInitiatives.getOnboardedInitiatives().stream().map(OnboardedInitiative::getInitiativeId).toList();
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
            return null;
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
