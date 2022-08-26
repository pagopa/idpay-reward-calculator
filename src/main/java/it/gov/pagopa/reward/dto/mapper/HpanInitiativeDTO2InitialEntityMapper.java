package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class HpanInitiativeDTO2InitialEntityMapper implements Function<HpanInitiativeDTO, HpanInitiatives> {
    @Override
    public HpanInitiatives apply(HpanInitiativeDTO hpanInitiativeDTO) {
        HpanInitiatives out = new HpanInitiatives();
        if (hpanInitiativeDTO.getOperationType().equals(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name())){
            out.setHpan(hpanInitiativeDTO.getHpan());
            out.setUserId(hpanInitiativeDTO.getUserId());
        }
        return out;
    }
}
