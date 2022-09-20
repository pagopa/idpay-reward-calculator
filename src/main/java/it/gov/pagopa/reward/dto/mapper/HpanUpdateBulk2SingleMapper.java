package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.BiFunction;

@Service
public class HpanUpdateBulk2SingleMapper implements BiFunction<HpanInitiativeBulkDTO, String, HpanUpdateEvaluateDTO> {
    @Override
    public HpanUpdateEvaluateDTO apply(HpanInitiativeBulkDTO hpanInitiativeBulkDTO, String hpan) {
        HpanUpdateEvaluateDTO output = new HpanUpdateEvaluateDTO();

        output.setUserId(hpanInitiativeBulkDTO.getUserId());
        output.setInitiativeId(hpanInitiativeBulkDTO.getInitiativeId());
        output.setHpan(hpan);
        output.setOperationType(hpanInitiativeBulkDTO.getOperationType());
        output.setEvaluationDate(LocalDateTime.now());

        return output;
    }
}