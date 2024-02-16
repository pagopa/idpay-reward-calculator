package it.gov.pagopa.reward.dto.synchronous;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class SynchronousTransactionAuthRequestDTO extends SynchronousTransactionRequestDTO {
    private long rewardCents;
}
