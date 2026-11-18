package it.gov.pagopa.reward.dto.build;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * InitiativeAdditionalDTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class InitiativeAdditionalDTO {
  @JsonProperty("logoURL")
  private String logoURL;

}
