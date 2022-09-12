package it.gov.pagopa.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HpanInitiativeDTO {
    private String userId;
    private String initiativeId;
    private String hpan;
    private String operationType;
    private LocalDateTime operationDate;

}
