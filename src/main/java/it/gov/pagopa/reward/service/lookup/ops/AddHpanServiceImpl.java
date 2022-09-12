package it.gov.pagopa.reward.service.lookup.ops;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class AddHpanServiceImpl implements AddHpanService {
    @Override
    public HpanInitiatives execute(HpanInitiatives hpanInitiatives, HpanInitiativeDTO hpanInitiativeDTO) {
        List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
        log.trace("[ADD_HPAN] Added evaluation for hpan: {}",hpanInitiativeDTO.getHpan());
        if (onboardedInitiatives != null){
            return executeHpanUpdate(hpanInitiatives, hpanInitiativeDTO, onboardedInitiatives);
        } else {
            return executeHpanCreate(hpanInitiatives, hpanInitiativeDTO);
        }
    }

    private HpanInitiatives executeHpanCreate(HpanInitiatives hpanInitiatives, HpanInitiativeDTO hpanInitiativeDTO) {
        log.trace("[ADD_HPAN] [NEW_HPAN] [HPAN_WITHOUT_ANY_INITIATIVE] Added evaluation for hpan: {} and add initiative: {}",hpanInitiativeDTO.getHpan(), hpanInitiativeDTO.getInitiativeId());
        OnboardedInitiative onboardedInitiative = getNewOnboardedInitiative(hpanInitiativeDTO);

        hpanInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative));
        return hpanInitiatives;
    }

    private HpanInitiatives executeHpanUpdate(HpanInitiatives hpanInitiatives, HpanInitiativeDTO hpanInitiativeDTO, List<OnboardedInitiative> onboardedInitiatives) {
        log.trace("[ADD_HPAN] [HPAN_PRESENT_IN_DB] [HPAN_WITHOUT_ANY_INITIATIVE] Added evaluation for hpan: {} and add initiative: {}",hpanInitiativeDTO.getHpan(),hpanInitiativeDTO.getInitiativeId());
        OnboardedInitiative onboardedInitiative = onboardedInitiatives.stream()//.map(OnboardedInitiative::getInitiativeId)
                .filter(o -> o.getInitiativeId().equals(hpanInitiativeDTO.getInitiativeId())).findFirst().orElse(null);
        if(onboardedInitiative!=null){
            List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();
            if (activeTimeIntervalsList.stream().noneMatch(activeTimeInterval -> hpanInitiativeDTO.getOperationDate().isBefore(activeTimeInterval.getStartInterval()))) {
                log.trace("[ADD_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_ALREADY_PRESENT] Added evaluation for hpan: {} and add initiative: {}", hpanInitiativeDTO.getHpan(), hpanInitiativeDTO.getInitiativeId());
                ActiveTimeInterval lastActiveInterval = activeTimeIntervalsList.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval)).orElse(null);

                if (lastActiveInterval != null) {
                    if(lastActiveInterval.getEndInterval()==null){
                        lastActiveInterval.setEndInterval(hpanInitiativeDTO.getOperationDate().with(LocalTime.MAX));
                        activeTimeIntervalsList.add(initializeInterval(hpanInitiativeDTO));
                        return hpanInitiatives;
                    } else if (lastActiveInterval.getEndInterval() != null && !lastActiveInterval.getEndInterval().isAfter(hpanInitiativeDTO.getOperationDate())) {
                        onboardedInitiative.setLastEndInterval(null);
                        activeTimeIntervalsList.add(initializeInterval(hpanInitiativeDTO));
                        return hpanInitiatives;
                    }
                }
                log.error("Unexpected use case, the initiative for this hpan not have an active interval. Source message: {} ", hpanInitiativeDTO);
                return null;
            }
            log.error("Unexpected use case, the hpan is before the last active interval. Source message: {}", hpanInitiativeDTO);
            return null;
        }else{
            log.trace("[ADD_HPAN] [HPAN_WITHOUT_INITIATIVE] Added evaluation for hpan: {} and add initiative: {}", hpanInitiativeDTO.getHpan(), hpanInitiativeDTO.getInitiativeId());
            onboardedInitiative = getNewOnboardedInitiative(hpanInitiativeDTO);
            hpanInitiatives.getOnboardedInitiatives().add(onboardedInitiative);
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
                .status("ACCEPTED")
                .acceptanceDate(hpanInitiativeDTO.getOperationDate().with(LocalTime.MIN).plusDays(1L))
                .activeTimeIntervals(List.of(initializeInterval(hpanInitiativeDTO)))
                .build();
    }
}
