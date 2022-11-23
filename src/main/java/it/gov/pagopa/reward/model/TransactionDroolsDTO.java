package it.gov.pagopa.reward.model;

import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TransactionDroolsDTO extends TransactionDTO {

    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();

    @Builder.Default
    private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    private List<String> initiatives;

    @Builder.Default
    private Map<String, Reward> rewards = new HashMap<>();
}

