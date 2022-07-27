package it.gov.pagopa.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reward {
    private BigDecimal providedReward;
    private BigDecimal accruedReward;
    private boolean capped;
}
