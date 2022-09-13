package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;

@Service
public class HpanUpdateBulk2SingleMapper implements BiFunction<HpanInitiativeBulkDTO, String, HpanInitiativeDTO> {
    @Override
    public HpanInitiativeDTO apply(HpanInitiativeBulkDTO hpanInitiativeBulkDTO, String hpan) {
        HpanInitiativeDTO output = new HpanInitiativeDTO();

        output.setUserId(hpanInitiativeBulkDTO.getUserId());
        output.setInitiativeId(hpanInitiativeBulkDTO.getInitiativeId());
        output.setHpan(hpan);
        output.setOperationType(hpanInitiativeBulkDTO.getOperationType());
        output.setOperationDate(hpanInitiativeBulkDTO.getOperationDate());

        return output;
    }
}
