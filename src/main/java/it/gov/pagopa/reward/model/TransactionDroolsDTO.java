package it.gov.pagopa.reward.model;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.TransactionDTO;
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
    List<String> rejectionReasons = new ArrayList<>();

    @Builder.Default
    Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    List<String> initiatives;

    @Builder.Default
    Map<String, Reward> rewards = new HashMap<>();
}

