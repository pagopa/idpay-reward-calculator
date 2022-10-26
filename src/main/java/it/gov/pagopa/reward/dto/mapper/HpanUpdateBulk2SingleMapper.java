package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.BiFunction;

@Service
public class HpanUpdateBulk2SingleMapper implements BiFunction<HpanInitiativeBulkDTO, PaymentMethodInfoDTO, HpanUpdateEvaluateDTO> {
    @Override
    public HpanUpdateEvaluateDTO apply(HpanInitiativeBulkDTO hpanInitiativeBulkDTO, PaymentMethodInfoDTO infoHpan) {
        HpanUpdateEvaluateDTO output = new HpanUpdateEvaluateDTO();

        output.setUserId(hpanInitiativeBulkDTO.getUserId());
        output.setInitiativeId(hpanInitiativeBulkDTO.getInitiativeId());
        output.setHpan(infoHpan.getHpan());
        output.setMaskedPan(infoHpan.getMaskedPan());
        output.setBrandLogo(infoHpan.getBrandLogo());
        output.setOperationType(hpanInitiativeBulkDTO.getOperationType());
        output.setEvaluationDate(LocalDateTime.now());

        return output;
    }
}
