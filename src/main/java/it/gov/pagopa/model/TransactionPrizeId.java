package it.gov.pagopa.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TransactionPrizeId{

    String idTrxAcquirer;

    String acquirerCode;

    String trxDate;

}
