package it.gov.pagopa.reward.dto.build;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

/**
 * InitiativeGeneralDTO
 */
@Validated
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2022-07-10T13:24:21.794Z[GMT]")

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class InitiativeGeneralDTO {

  @JsonProperty("name")
  private String name;

  @JsonProperty("budgetCents")
  private Long budgetCents;

  /**
   * Gets or Sets beneficiaryType
   */
  public enum BeneficiaryTypeEnum {
    PF("PF"),
    
    PG("PG"),
    NF("NF");

    private String value;

    BeneficiaryTypeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static BeneficiaryTypeEnum fromValue(String text) {
      for (BeneficiaryTypeEnum b : BeneficiaryTypeEnum.values()) {
        if (String.valueOf(b.value).equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("beneficiaryType")
  private BeneficiaryTypeEnum beneficiaryType;

  @JsonProperty("beneficiaryKnown")
  private Boolean beneficiaryKnown;

  @JsonProperty("beneficiaryBudgetCents")
  private Long beneficiaryBudgetCents;

  @JsonProperty("startDate")
  private LocalDate startDate;

  @JsonProperty("endDate")
  private LocalDate endDate;

  @JsonProperty("rankingStartDate")
  private LocalDate rankingStartDate;

  @JsonProperty("rankingEndDate")
  private LocalDate rankingEndDate;

}
