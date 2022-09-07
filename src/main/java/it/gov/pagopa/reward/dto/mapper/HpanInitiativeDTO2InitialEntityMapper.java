package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class HpanInitiativeDTO2InitialEntityMapper implements Function<HpanInitiativeDTO, HpanInitiatives> {
    @Override
    public HpanInitiatives apply(HpanInitiativeDTO hpanInitiativeDTO) {
        HpanInitiatives out = new HpanInitiatives();
        if (hpanInitiativeDTO.getOperationType().equals(HpanInitiativeConstants.ADD_INSTRUMENT)){
            out.setHpan(hpanInitiativeDTO.getHpan());
            out.setUserId(hpanInitiativeDTO.getUserId());
        }
        return out;
    }
}
