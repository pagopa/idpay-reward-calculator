package it.gov.pagopa.reward.model;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "reward_rule")
public class DroolsRule {
    @Id
    private String id;
    private String name;
    private String rule;
    private String ruleVersion;
    private InitiativeConfig initiativeConfig;
    private LocalDateTime updateDate;
}
