package it.gov.pagopa.reward.service.lookup.ops;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
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
    public OnboardedInitiative execute(HpanInitiatives hpanInitiatives, HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
        log.trace("[ADD_HPAN] Added evaluation for hpan: {}", hpanUpdateEvaluateDTO.getHpan());
        if (onboardedInitiatives != null){
            return executeHpanUpdate(hpanInitiatives, hpanUpdateEvaluateDTO, onboardedInitiatives);
        } else {
            return executeHpanCreate(hpanUpdateEvaluateDTO);
        }
    }

    private OnboardedInitiative executeHpanCreate(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        log.trace("[ADD_HPAN] [NEW_HPAN] [HPAN_WITHOUT_ANY_INITIATIVE] Added evaluation for hpan: {} and add initiative: {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
        return getNewOnboardedInitiative(hpanUpdateEvaluateDTO);
    }

    private OnboardedInitiative executeHpanUpdate(HpanInitiatives hpanInitiatives, HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO, List<OnboardedInitiative> onboardedInitiatives) {
        log.trace("[ADD_HPAN] [HPAN_PRESENT_IN_DB] [HPAN_WITHOUT_ANY_INITIATIVE] Added evaluation for hpan: {} and add initiative: {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
        OnboardedInitiative onboardedInitiative = onboardedInitiatives.stream()//.map(OnboardedInitiative::getInitiativeId)
                .filter(o -> o.getInitiativeId().equals(hpanUpdateEvaluateDTO.getInitiativeId())).findFirst().orElse(null);
        if(onboardedInitiative!=null){
            List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();
            if (activeTimeIntervalsList.stream().noneMatch(activeTimeInterval -> hpanUpdateEvaluateDTO.getEvaluationDate().isBefore(activeTimeInterval.getStartInterval()))) {
                log.trace("[ADD_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_ALREADY_PRESENT] Added evaluation for hpan: {} and add initiative: {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
                ActiveTimeInterval lastActiveInterval = activeTimeIntervalsList.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval)).orElse(null);

                if (lastActiveInterval != null) {
                    if(lastActiveInterval.getEndInterval()==null){
                        lastActiveInterval.setEndInterval(hpanUpdateEvaluateDTO.getEvaluationDate().with(LocalTime.MAX));
                        activeTimeIntervalsList.add(initializeInterval(hpanUpdateEvaluateDTO));

                        return onboardedInitiative;
                    } else if (!lastActiveInterval.getEndInterval().isAfter(hpanUpdateEvaluateDTO.getEvaluationDate())) {
                        onboardedInitiative.setLastEndInterval(null);
                        activeTimeIntervalsList.add(initializeInterval(hpanUpdateEvaluateDTO));

                        return onboardedInitiative;
                    }
                }
                log.error("Unexpected use case, the initiative for this hpan not have an active interval. Source message: {} ", hpanUpdateEvaluateDTO);
                return null;
            }
            log.error("Unexpected use case, the hpan is before the last active interval. Source message: {}", hpanUpdateEvaluateDTO);
            return null;
        }else{
            log.trace("[ADD_HPAN] [HPAN_WITHOUT_INITIATIVE] Added evaluation for hpan: {} and add initiative: {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
            onboardedInitiative = getNewOnboardedInitiative(hpanUpdateEvaluateDTO);
            hpanInitiatives.getOnboardedInitiatives().add(onboardedInitiative);

            return onboardedInitiative;
        }
    }

    private ActiveTimeInterval initializeInterval(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        return ActiveTimeInterval.builder()
                .startInterval(hpanUpdateEvaluateDTO.getEvaluationDate().with(LocalTime.MIN).plusDays(1L))
                .build();
    }

    private OnboardedInitiative getNewOnboardedInitiative(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        return OnboardedInitiative.builder()
                .initiativeId(hpanUpdateEvaluateDTO.getInitiativeId())
                .status("ACCEPTED")
                .acceptanceDate(hpanUpdateEvaluateDTO.getEvaluationDate().with(LocalTime.MIN).plusDays(1L))
                .activeTimeIntervals(List.of(initializeInterval(hpanUpdateEvaluateDTO)))
                .build();
    }
}
