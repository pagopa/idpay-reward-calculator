package it.gov.pagopa.reward.model.counters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "user_initiative_counters")
public class UserInitiativeCounters {
    @Id
    private String userId;
    private List<InitiativeCounters> initiatives;
}
