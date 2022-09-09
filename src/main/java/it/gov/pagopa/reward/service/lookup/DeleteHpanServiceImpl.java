package it.gov.pagopa.reward.service.lookup;

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
public class DeleteHpanServiceImpl implements DeleteHpanService{
    @Override
    public HpanInitiatives execute(HpanInitiatives hpanInitiatives, HpanInitiativeDTO hpanInitiativeDTO) {
        if (hpanInitiatives.getHpan() != null) {
            List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
            log.trace("[DELETED_HPAN] Is presente in db the follow hpan: {}",hpanInitiativeDTO.getHpan());
            if (onboardedInitiatives != null){
               OnboardedInitiative onboardedInitiative = onboardedInitiatives.stream().filter(o -> o.getInitiativeId().equals(hpanInitiativeDTO.getInitiativeId())).findFirst().orElse(null);
                if (onboardedInitiative!=null) {
                    return evaluateHpanWithInitiativePresent(hpanInitiatives, hpanInitiativeDTO, onboardedInitiative);
                }
                log.debug("The hpan has no reference to the initiative. Source message: {}", hpanInitiativeDTO);
                return null;
            }
            log.error("Unexpected use case, the hpan not have any initiatives associate. Source message: {}", hpanInitiativeDTO);
            return null;
        }
        log.debug("The hpan is not present into DB. Source message: {}", hpanInitiativeDTO);
        return null;

    }

    private HpanInitiatives evaluateHpanWithInitiativePresent(HpanInitiatives hpanInitiatives, HpanInitiativeDTO hpanInitiativeDTO, OnboardedInitiative onboardedInitiative) {
        log.trace("[DELETED_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_IS_PRESENT] The hpan: {}, contain the initiative: {}", hpanInitiativeDTO.getHpan(), hpanInitiativeDTO.getInitiativeId());
        List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.getActiveTimeIntervals();
        if(activeTimeIntervalsList != null) {
            ActiveTimeInterval lastActiveInterval = activeTimeIntervalsList.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval)).orElse(null);
            if (lastActiveInterval != null) {
                if (!hpanInitiativeDTO.getOperationDate().isBefore(lastActiveInterval.getStartInterval())
                        && (lastActiveInterval.getEndInterval() == null
                        || hpanInitiativeDTO.getOperationDate().isAfter(lastActiveInterval.getEndInterval()))) {
                    lastActiveInterval.setEndInterval(hpanInitiativeDTO.getOperationDate().with(LocalTime.MAX));
                    return hpanInitiatives;
                }
                log.debug("The hpan is before the last active interval, Source message: {}", hpanInitiativeDTO);
                return null;
            }
            log.error("Unexpected case, the initiative have a empty actives interval. Source message: {} ", hpanInitiativeDTO);
            return null;
        }
        log.error("Unexpected case, the initiative for this hpan not have an active interval. Source message: {} ", hpanInitiativeDTO);
        return null;
    }
}
