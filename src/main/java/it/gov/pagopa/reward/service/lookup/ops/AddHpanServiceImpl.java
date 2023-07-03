package it.gov.pagopa.reward.service.lookup.ops;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class AddHpanServiceImpl implements AddHpanService {
    @Override
    public OnboardedInitiative execute(HpanInitiatives hpanInitiatives, HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO, boolean recessFlow) {
        List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
        log.trace("[ADD_HPAN] Added evaluation for hpan: {}", hpanUpdateEvaluateDTO.getHpan());
        if (onboardedInitiatives != null){
            return executeHpanUpdate(hpanInitiatives, hpanUpdateEvaluateDTO, onboardedInitiatives, recessFlow);
        } else {
            return executeHpanCreate(hpanUpdateEvaluateDTO);
        }
    }

    private OnboardedInitiative executeHpanCreate(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        log.trace("[ADD_HPAN] [NEW_HPAN] [HPAN_WITHOUT_ANY_INITIATIVE] Added evaluation for hpan: {} and add initiative: {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
        return getNewOnboardedInitiative(hpanUpdateEvaluateDTO);
    }

    private OnboardedInitiative executeHpanUpdate(HpanInitiatives hpanInitiatives, HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO, List<OnboardedInitiative> onboardedInitiatives, boolean recessFlow) {
        log.trace("[ADD_HPAN] [HPAN_PRESENT_IN_DB] [HPAN_WITHOUT_ANY_INITIATIVE] Added evaluation for hpan: {} and add initiative: {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
        OnboardedInitiative onboardedInitiative = onboardedInitiatives.stream()//.map(OnboardedInitiative::getInitiativeId)
                .filter(o -> o.getInitiativeId().equals(hpanUpdateEvaluateDTO.getInitiativeId())).findFirst().orElse(null);
        if(onboardedInitiative!=null){
            if(!recessFlow && HpanInitiativeConstants.STATUS_INACTIVE.equals(onboardedInitiative.getStatus())){
                log.error("Unexpected use case, the user unsubscribe from the initiative. Source message: {} ", hpanUpdateEvaluateDTO);
                return null;
            }
            List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();
            LocalDateTime startInterval = hpanUpdateEvaluateDTO.getEvaluationDate();

            if (activeTimeIntervalsList.stream().noneMatch(activeTimeInterval -> startInterval.isBefore(activeTimeInterval.getStartInterval()))) {
                log.trace("[ADD_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_ALREADY_PRESENT] Added evaluation for hpan: {} and add initiative: {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
                ActiveTimeInterval lastActiveInterval = activeTimeIntervalsList.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval)).orElse(null);

                if (lastActiveInterval != null) {
                    if(lastActiveInterval.getEndInterval()==null){
                        lastActiveInterval.setEndInterval(startInterval);
                        activeTimeIntervalsList.add(initializeInterval(startInterval));
                        onboardedInitiative.setStatus(HpanInitiativeConstants.STATUS_UPDATE);

                        return onboardedInitiative;
                    } else if (!lastActiveInterval.getEndInterval().isAfter(startInterval)) {
                        onboardedInitiative.setLastEndInterval(null);
                        activeTimeIntervalsList.add(initializeInterval(startInterval));
                        onboardedInitiative.setStatus(HpanInitiativeConstants.STATUS_UPDATE);

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
            onboardedInitiative.setStatus(HpanInitiativeConstants.STATUS_ACTIVE);

            return onboardedInitiative;
        }
    }

    private ActiveTimeInterval initializeInterval(LocalDateTime startInterval) {
        return ActiveTimeInterval.builder()
                .startInterval(startInterval)
                .build();
    }

    private OnboardedInitiative getNewOnboardedInitiative(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        LocalDateTime startInterval = hpanUpdateEvaluateDTO.getEvaluationDate();
        return OnboardedInitiative.builder()
                .initiativeId(hpanUpdateEvaluateDTO.getInitiativeId())
                .status(HpanInitiativeConstants.STATUS_ACTIVE)
                .acceptanceDate(startInterval)
                .activeTimeIntervals(new ArrayList<>(List.of(initializeInterval(startInterval))))
                .build();
    }
}
