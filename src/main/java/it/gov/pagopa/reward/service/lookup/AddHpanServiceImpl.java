package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j
public class AddHpanServiceImpl implements AddHpanService{
    @Override
    public HpanInitiatives execute(HpanInitiatives hpanInitiatives, HpanInitiativeDTO hpanInitiativeDTO) {
        List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
        log.trace("[ADD_HPAN] Added evaluation for hpan: {}",hpanInitiativeDTO.getHpan());
        if (onboardedInitiatives != null){
            Optional<OnboardedInitiative> initiative = onboardedInitiatives.stream()//.map(OnboardedInitiative::getInitiativeId)
                    .filter(o -> o.getInitiativeId().equals(hpanInitiativeDTO.getInitiativeId())).findFirst();
            if(initiative.isPresent()){
                List<ActiveTimeInterval> intervalsList = hpanInitiatives.getOnboardedInitiatives().stream()
                    .filter(h -> h.getInitiativeId().equals(hpanInitiativeDTO.getInitiativeId()))
                    .map(OnboardedInitiative::getActiveTimeIntervals)
                    .toList().get(0);
                if (intervalsList.stream().noneMatch(activeTimeInterval -> hpanInitiativeDTO.getOperationDate().isBefore(activeTimeInterval.getStartInterval()))) {
                    log.trace("[ADD_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_ALREADY_PRESENT] Added evaluation for hpan: %s and add initiative: %s".formatted(hpanInitiativeDTO.getHpan(),hpanInitiativeDTO.getInitiativeId()));
                    OnboardedInitiative onboardedInitiative = initiative.get();
                    List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();

                    Optional<ActiveTimeInterval> lastActiveInterval = activeTimeIntervalsList.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval));
                    if (lastActiveInterval.isPresent() && lastActiveInterval.get().getEndInterval() != null && lastActiveInterval.get().getEndInterval().isBefore(hpanInitiativeDTO.getOperationDate())) {
                        onboardedInitiative.getActiveTimeIntervals()
                                .add(initializeInterval(hpanInitiativeDTO));
                        return hpanInitiatives;
                    }else if(lastActiveInterval.isPresent() && lastActiveInterval.get().getEndInterval()==null){
                        lastActiveInterval.get().setEndInterval(hpanInitiativeDTO.getOperationDate().with(LocalTime.MAX));
                        activeTimeIntervalsList.add(initializeInterval(hpanInitiativeDTO));
                        return hpanInitiatives;
                    }
                }
            }else{
                OnboardedInitiative onboardedInitiative = getNewOnboardedInitiative(hpanInitiativeDTO);
                hpanInitiatives.getOnboardedInitiatives().add(onboardedInitiative);
                return hpanInitiatives;
            }
            log.error("Unexpected case for update hpan: {}",hpanInitiativeDTO);
            return null;
        } else {
            log.trace("[ADD_HPAN] [HPAN_WITHOUT_INITIATIVE] Added evaluation for hpan: %s and add initiative: %s".formatted(hpanInitiativeDTO.getHpan(),hpanInitiativeDTO.getInitiativeId()));
            OnboardedInitiative onboardedInitiative = getNewOnboardedInitiative(hpanInitiativeDTO);

            hpanInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative));
            return hpanInitiatives;
        }
    }

    private ActiveTimeInterval initializeInterval(HpanInitiativeDTO hpanInitiativeDTO) {
        return ActiveTimeInterval.builder()
                .startInterval(hpanInitiativeDTO.getOperationDate().with(LocalTime.MIN).plusDays(1L))
                .build();
    }

    private OnboardedInitiative getNewOnboardedInitiative(HpanInitiativeDTO hpanInitiativeDTO) {
        return OnboardedInitiative.builder()
                .initiativeId(hpanInitiativeDTO.getInitiativeId())
//                    .acceptanceDate(dto.getOperationDate().toLocalDate())
                .status("ACCEPTED")
                .activeTimeIntervals(List.of(initializeInterval(hpanInitiativeDTO)))
                .build();
    }
}
