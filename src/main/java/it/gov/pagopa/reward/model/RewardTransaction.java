package it.gov.pagopa.reward.model;

import it.gov.pagopa.reward.dto.TransactionDTO;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "rewardTransactionBuilder")
@EqualsAndHashCode(callSuper = true)
public class RewardTransaction extends TransactionDTO {

    String status;

    String rejectionReason;

    List<String> initiatives;

    Map<String,BigDecimal> rewards;
}

