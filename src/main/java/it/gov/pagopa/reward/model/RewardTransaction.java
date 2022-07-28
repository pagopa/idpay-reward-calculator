package it.gov.pagopa.reward.model;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.Reward;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderMethodName = "rewardTransactionBuilder")
@EqualsAndHashCode(callSuper = true)
public class RewardTransaction extends TransactionDTO {

    String status;

    List<String> rejectionReason = new ArrayList<>();

    List<String> initiatives;

    Map<String, Reward> rewards;
}

