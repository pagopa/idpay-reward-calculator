package it.gov.pagopa.reward.service.lookup.ops;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class DeleteHpanServiceImpl implements DeleteHpanService {
    @Override
    public OnboardedInitiative execute(HpanInitiatives hpanInitiatives, HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
            List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
            log.trace("[DELETED_HPAN] Is presente in db the follow hpan: {}", hpanUpdateEvaluateDTO.getHpan());
            if (onboardedInitiatives != null){
               OnboardedInitiative onboardedInitiative = onboardedInitiatives.stream().filter(o -> o.getInitiativeId().equals(hpanUpdateEvaluateDTO.getInitiativeId())).findFirst().orElse(null);
                if (onboardedInitiative!=null) {
                    return evaluateHpanWithInitiativePresent(hpanUpdateEvaluateDTO, onboardedInitiative);
                } else{
                    log.error("Unexpected use case, the hpan has no reference to the initiative. Source message: {}", hpanUpdateEvaluateDTO);
                    return null;
                }
            } else{
                log.error("Unexpected use case, the hpan not have any initiatives associate. Source message: {}", hpanUpdateEvaluateDTO);
                return null;
            }
    }

    private OnboardedInitiative evaluateHpanWithInitiativePresent(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO, OnboardedInitiative onboardedInitiative) {
        log.trace("[DELETED_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_IS_PRESENT] The hpan: {}, contain the initiative: {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
        List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();
        if(activeTimeIntervalsList != null) {
            ActiveTimeInterval lastActiveInterval = activeTimeIntervalsList.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval)).orElse(null);
            if (lastActiveInterval != null) {
                LocalDateTime endInterval = hpanUpdateEvaluateDTO.getEvaluationDate().plusDays(1).with(LocalTime.MIN);
                if (endInterval.isAfter(lastActiveInterval.getStartInterval())){
                    if (lastActiveInterval.getEndInterval() == null) {
                            lastActiveInterval.setEndInterval(endInterval);
                            onboardedInitiative.setLastEndInterval(endInterval);

                            return onboardedInitiative;
                        }
                        log.error("Unexpected use case, the initiative for this hpan not have an active interval open. Source message: {}", hpanUpdateEvaluateDTO);
                        return null;
                }else if(endInterval.equals(lastActiveInterval.getStartInterval())){
                    log.debug("[DELETED_HPAN] deleting interval before its start: {} , {}", hpanUpdateEvaluateDTO.getHpan(), hpanUpdateEvaluateDTO.getInitiativeId());
                    activeTimeIntervalsList.remove(lastActiveInterval);
                    return onboardedInitiative;
                }
                log.error("Unexpected use case, the hpan is before the last active interval, Source message: {}", hpanUpdateEvaluateDTO);
                return null;
            }
            log.error("Unexpected use case, the initiative have a empty actives interval. Source message: {} ", hpanUpdateEvaluateDTO);
            return null;
        }
        log.error("Unexpected use case, the initiative for this hpan not have an active interval. Source message: {} ", hpanUpdateEvaluateDTO);
        return null;
    }
}
