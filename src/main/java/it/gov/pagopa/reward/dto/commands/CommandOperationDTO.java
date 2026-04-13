package it.gov.pagopa.reward.dto.commands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class CommandOperationDTO {
    private String entityId;
    private String operationType;
    private Instant operationTime;
}
