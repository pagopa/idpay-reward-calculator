package it.gov.pagopa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardsTransactionDTO {
    TransactionDTO transaction;
    Map<String, BigDecimal> rewards;
}
