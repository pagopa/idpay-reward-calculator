package it.gov.pagopa.reward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Initiative {
    private String initiativeId;
    private LocalDate acceptanceDate;
    private Long budget;
    private BigDecimal accrued;
    private Long trxCount;
    private String status;
    private List<String> hpanActive;
}
