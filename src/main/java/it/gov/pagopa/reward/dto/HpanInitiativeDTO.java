package it.gov.pagopa.reward.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class HpanInitiativeDTO {
    private String initiativeId;
    private String hpan;
    private LocalDateTime timestamp;
    private String userId;
    private OperationType operationType;

    public enum OperationType{
        ADD_INSTRUMENT,
        DELETE_INSTRUMENT
    }
}
