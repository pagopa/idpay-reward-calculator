package it.gov.pagopa.model;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = {"idTrxAcquirer","acquirerCode", "trxDate"}, callSuper = false)
@Table("transaction")
public class TransactionPrize {


    @Column(value = "id_trx_acquirer_s")
    String idTrxAcquirer;

    @Column(value = "acquirer_c")
    String acquirerCode;

    @Column(value = "trx_date_s")
    String trxDate;

    @Column(value = "hpan_s")
    String hpan;

    @Column(value = "operation_type_s")
    String operationType;

    @Column(value = "circuit_type_s")
    String circuitType;

    @Column(value = "id_trx_issuer_s")
    String idTrxIssuer;

    @Column(value = "correlation_id_s")
    String correlationId;

    @Column(value = "amount_n")
    BigDecimal amount;

    @Column(value = "amount_currency_s")
    String amountCurrency;

    @Column(value = "mcc_c")
    String mcc;

    @Column(value = "acquirer_id_s")
    String acquirerId;

    @Column(value = "merchant_id_s")
    String merchantId;

    @Column(value = "terminal_id_s")
    String terminalId;

    @Column(value = "bin_s")
    String bin;

    @Column(value = "prize_n")
    BigDecimal prize;
}

