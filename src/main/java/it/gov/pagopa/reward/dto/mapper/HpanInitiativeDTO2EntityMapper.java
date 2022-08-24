package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class HpanInitiativeDTO2EntityMapper implements Function<HpanInitiativeDTO, HpanInitiatives> {
    @Override
    public HpanInitiatives apply(HpanInitiativeDTO hpanInitiativeDTO) {

        if (hpanInitiativeDTO.getOperationType().equals(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name())){
            HpanInitiatives out = new HpanInitiatives();

            out.setHpan(hpanInitiativeDTO.getHpan());
            out.setUserId(hpanInitiativeDTO.getUserId());
            OnboardedInitiative onboardedInitiative= new OnboardedInitiative();
            onboardedInitiative.setInitiativeId(hpanInitiativeDTO.getInitiativeId());

            //TODO remember to check other field from OnboardedInitiative

            ActiveTimeInterval interval = new ActiveTimeInterval();
            interval.setStartInterval(hpanInitiativeDTO.getOperationDate());
            onboardedInitiative.setActiveTimeIntervals(List.of(interval));
            out.setOnboardedInitiatives(List.of(onboardedInitiative));
            return out;
        }
        return null;
    }
}
