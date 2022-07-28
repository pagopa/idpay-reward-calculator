package it.gov.pagopa.reward.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RewardTransactionDTO extends TransactionDTO {

    String status;

    @Builder.Default
    List<String> rejectionReasons = new ArrayList<>();

    @Builder.Default
    Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    List<String> initiatives;

    Map<String, Reward> rewards;
}
