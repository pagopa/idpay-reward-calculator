package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateOutcomeDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class HpanList2HpanUpdateOutcomeDTOMapper implements TriFunction<List<String>, HpanInitiativeBulkDTO, LocalDateTime, HpanUpdateOutcomeDTO> {
    @Override
    public HpanUpdateOutcomeDTO apply(List<String> hpanList, HpanInitiativeBulkDTO payload, LocalDateTime evaluationDate) {
        List<String> hpanRejected = new ArrayList<>(payload.getInfoList().stream().map(PaymentMethodInfoDTO::getHpan).toList());
        hpanRejected.removeAll(hpanList);
        return HpanUpdateOutcomeDTO.builder()
                .initiativeId(payload.getInitiativeId())
                .userId(payload.getUserId())
                .hpanList(hpanList)
                .rejectedHpanList(hpanRejected)
                .operationType(payload.getOperationType())
                .timestamp(evaluationDate)
                .build();
    }
}