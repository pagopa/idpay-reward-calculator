package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2InitialEntityMapper;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class HpanInitiativesServiceImpl implements HpanInitiativesService{

    private final HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper;

    public HpanInitiativesServiceImpl(HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper) {
        this.hpanInitiativeDTO2InitialEntityMapper = hpanInitiativeDTO2InitialEntityMapper;
    }

    @Override
    public Mono<HpanInitiatives> hpanInitiativeUpdateInformation(Pair<HpanInitiativeDTO, Mono<HpanInitiatives>> hpanInitiativePair) {
        HpanInitiativeDTO hpanInitiativeDTO = hpanInitiativePair.getFirst();
        Mono<HpanInitiatives> hpanRetrieved = hpanInitiativePair.getSecond();

        return hpanRetrieved
                .defaultIfEmpty(hpanInitiativeDTO2InitialEntityMapper.apply(hpanInitiativeDTO))
                .mapNotNull(h-> evaluate(h, hpanInitiativeDTO));
    }

    private HpanInitiatives evaluate(HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto){
        if (dto.getOperationType().equals(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name())){
            return addedHpanInitiative(hpanInitiatives, dto);
        }else{
            return deletedHpanInitiative(hpanInitiatives, dto);
        }
    }
    private HpanInitiatives addedHpanInitiative(HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto){
        List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
        //add hpan non presente --> è stato popolato con uno di default visto in presedenza
        if (onboardedInitiatives != null
                && onboardedInitiatives.stream().map(OnboardedInitiative::getInitiativeId).filter(id -> id.equals(dto.getInitiativeId())).count() == 1L) {
            List<ActiveTimeInterval> intervalsList = hpanInitiatives.getOnboardedInitiatives().stream()
                    .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId()))
                    .map(OnboardedInitiative::getActiveTimeIntervals)
                    .toList().get(0);
            if (intervalsList.stream().noneMatch(activeTimeInterval -> dto.getOperationDate().isBefore(activeTimeInterval.getStartInterval()))) {
                OnboardedInitiative onboardedInitiative = hpanInitiatives.getOnboardedInitiatives().stream()
                        .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId())).toList().get(0);

                List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();

                LocalDateTime lastActivate = Collections.max(activeTimeIntervalsList.stream().map(ActiveTimeInterval::getStartInterval).toList());
                ActiveTimeInterval lastActiveInterval = activeTimeIntervalsList.stream().filter(activeTimeInterval -> activeTimeInterval.getStartInterval().equals(lastActivate)).toList().get(0);

                if (lastActiveInterval.getEndInterval() != null) {
                    onboardedInitiative.getActiveTimeIntervals()
                            .add(ActiveTimeInterval.builder()
                                    .startInterval(dto.getOperationDate())
                                    .build());
                    return hpanInitiatives;
                }
            }
            return null;
        } else {
            OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                    .initiativeId(dto.getInitiativeId())
                    .acceptanceDate(dto.getOperationDate().toLocalDate())
                    .status("ACCEPTED")
                    .activeTimeIntervals(List.of(ActiveTimeInterval.builder().startInterval(dto.getOperationDate()).build()))
                    .build();

            if(hpanInitiatives.getOnboardedInitiatives() == null) {
                List<OnboardedInitiative> newList = new ArrayList<>();
                newList.add(onboardedInitiative);
                hpanInitiatives.setOnboardedInitiatives(newList);
            }else {
                hpanInitiatives.getOnboardedInitiatives().add(onboardedInitiative);
            }
            return hpanInitiatives;
        }
    }

    private HpanInitiatives deletedHpanInitiative(HpanInitiatives hpanInitiatives, HpanInitiativeDTO dto) {
        if (hpanInitiatives.getHpan() != null) {
            List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();

            if (onboardedInitiatives != null
                    && onboardedInitiatives.stream().map(OnboardedInitiative::getInitiativeId).filter(id -> id.equals(dto.getInitiativeId())).count() == 1L) {
                OnboardedInitiative onboardedInitiative = onboardedInitiatives.stream()
                        .filter(h -> h.getInitiativeId().equals(dto.getInitiativeId())).toList().get(0);

                List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();

                LocalDateTime lastActivate = Collections.max(activeTimeIntervalsList.stream().map(ActiveTimeInterval::getStartInterval).toList());
                List<ActiveTimeInterval> lastActiveInterval = activeTimeIntervalsList.stream().filter(activeTimeInterval -> activeTimeInterval.getStartInterval().equals(lastActivate)).toList();

                if (dto.getOperationDate().isAfter(lastActivate)
                        && lastActiveInterval.get(0).getEndInterval() == null) {

                    onboardedInitiative.getActiveTimeIntervals().remove(ActiveTimeInterval.builder()
                            .startInterval(lastActivate)
                            .build());

                    onboardedInitiative.getActiveTimeIntervals().add(ActiveTimeInterval.builder()
                            .startInterval(lastActivate)
                            .endInterval(dto.getOperationDate())
                            .build());

                    return hpanInitiatives;
                }
            }
        }
        return null;
    }
}
