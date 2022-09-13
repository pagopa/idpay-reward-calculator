package it.gov.pagopa.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HpanInitiativeBulkDTO {
    private String userId;
    private String initiativeId;
    private List<String> hpanList;
    private String operationType;
    private LocalDateTime operationDate;
}
