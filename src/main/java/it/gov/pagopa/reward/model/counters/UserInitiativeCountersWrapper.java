package it.gov.pagopa.reward.model.counters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class UserInitiativeCountersWrapper {
    private String entityId;
    private Map<String, UserInitiativeCounters> initiatives;
}
