package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class HpanUpdateEvaluateDTO2HpanInitiativeMapper implements Function<HpanUpdateEvaluateDTO, HpanInitiatives> {
    @Override
    public HpanInitiatives apply(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO) {
        HpanInitiatives out = null;
        if (hpanUpdateEvaluateDTO.getOperationType().equals(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)){
            out = new HpanInitiatives();
            out.setHpan(hpanUpdateEvaluateDTO.getHpan());
            out.setMaskedPan(hpanUpdateEvaluateDTO.getMaskedPan());
            out.setBrandLogo(hpanUpdateEvaluateDTO.getBrandLogo());
            out.setBrand(hpanUpdateEvaluateDTO.getBrand());
            out.setUserId(hpanUpdateEvaluateDTO.getUserId());
        }
        return out;
    }
}
