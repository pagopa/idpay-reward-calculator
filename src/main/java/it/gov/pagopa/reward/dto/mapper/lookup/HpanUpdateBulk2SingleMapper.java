package it.gov.pagopa.reward.dto.mapper.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HpanUpdateBulk2SingleMapper implements TriFunction<HpanInitiativeBulkDTO, PaymentMethodInfoDTO, LocalDateTime, HpanUpdateEvaluateDTO> {
    @Override
    public HpanUpdateEvaluateDTO apply(HpanInitiativeBulkDTO hpanInitiativeBulkDTO, PaymentMethodInfoDTO infoHpan, LocalDateTime evaluationDate) {
        HpanUpdateEvaluateDTO output = new HpanUpdateEvaluateDTO();

        output.setUserId(hpanInitiativeBulkDTO.getUserId());
        output.setInitiativeId(hpanInitiativeBulkDTO.getInitiativeId());
        output.setHpan(infoHpan.getHpan());
        output.setMaskedPan(infoHpan.getMaskedPan());
        output.setBrandLogo(infoHpan.getBrandLogo());
        output.setBrand(infoHpan.getBrand());
        output.setOperationType(hpanInitiativeBulkDTO.getOperationType());
        output.setEvaluationDate(evaluationDate);

        return output;
    }
}
