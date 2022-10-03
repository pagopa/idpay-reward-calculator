package it.gov.pagopa.reward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveTimeInterval {
    private LocalDateTime startInterval;
    private LocalDateTime endInterval;
}
