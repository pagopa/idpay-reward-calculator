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
import java.util.Optional;

@Service
@Slf4j
public class DeleteHpanServiceImpl implements DeleteHpanService{
    @Override
    public HpanInitiatives execute(HpanInitiatives hpanInitiatives, HpanInitiativeDTO hpanInitiativeDTO) {
        if (hpanInitiatives.getHpan() != null) {
            List<OnboardedInitiative> onboardedInitiatives = hpanInitiatives.getOnboardedInitiatives();
            log.trace("[DELETED_HPAN] Is presente in db the follow hpan: {}",hpanInitiativeDTO.getHpan());
            if (onboardedInitiatives != null){
                Optional<OnboardedInitiative> onboardedInitiative = onboardedInitiatives.stream().filter(o -> o.getInitiativeId().equals(hpanInitiativeDTO.getInitiativeId())).findFirst();
                if (onboardedInitiative.isPresent()) {
                    log.trace("[DELETED_HPAN] [HPAN_WITH_INITIATIVE] [INITIATIVE_IS_PRESENT] The hpan: %s, contain the initiative: %s".formatted(hpanInitiativeDTO.getHpan(),hpanInitiativeDTO.getInitiativeId()));
                    List<ActiveTimeInterval> activeTimeIntervalsList = onboardedInitiative.get().getActiveTimeIntervals();

                    Optional<ActiveTimeInterval> lastActiveInterval = activeTimeIntervalsList.stream().max(Comparator.comparing(ActiveTimeInterval::getStartInterval));

                    if (lastActiveInterval.isPresent() &&
                            hpanInitiativeDTO.getOperationDate().isAfter(lastActiveInterval.get().getStartInterval())
                            && (lastActiveInterval.get().getEndInterval() == null
                            || hpanInitiativeDTO.getOperationDate().isAfter(lastActiveInterval.get().getEndInterval()))) {
                        lastActiveInterval.get().setEndInterval(hpanInitiativeDTO.getOperationDate().with(LocalTime.MAX));
                        return hpanInitiatives;
                    }
                }
            }
        }
        log.error("Unexpected case for evaluate delete hpan: {}", hpanInitiativeDTO);
        return null;
    }
}
