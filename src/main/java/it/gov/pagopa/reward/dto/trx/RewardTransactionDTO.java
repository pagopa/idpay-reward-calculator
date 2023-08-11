package it.gov.pagopa.reward.dto.trx;

import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** When stored into mongo, it will represent a transaction that has NOT been elaborated */
@Data
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Document(collection = "transactions_processed")
public class RewardTransactionDTO extends TransactionDTO implements BaseTransactionProcessed {

    private String status;

    @Builder.Default
    private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    private Map<String, Reward> rewards;
    private List<String> initiatives;

    @Builder.Default
    private LocalDateTime elaborationDateTime = LocalDateTime.now();
}
