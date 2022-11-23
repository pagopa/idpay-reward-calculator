package it.gov.pagopa.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HpanUpdateOutcomeDTO {
    private String initiativeId;
    private String userId;
    private List<String> hpanList;
    private List<String> rejectedHpanList;
    private String operationType;
    private LocalDateTime timestamp;
}