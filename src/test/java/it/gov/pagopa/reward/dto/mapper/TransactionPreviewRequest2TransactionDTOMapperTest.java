package it.gov.pagopa.reward.dto.mapper;


import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousRequest;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class TransactionPreviewRequest2TransactionDTOMapperTest {

    @Test
    void applyTest(){
        // Give
        String operationType = "00";
        TransactionPreviewRequest2TransactionDTOMapper mapper = new TransactionPreviewRequest2TransactionDTOMapper(operationType);
        TransactionSynchronousRequest previewRequest = TransactionSynchronousRequest.builder()
                .transactionId("TRANSACTIONID")
                .userId("USERID")
                .merchantId("MERCHANTID")
                .senderCode("SENDERCODE")
                .merchantFiscalCode("MERCHANTFISCALCODE")
                .vat("VAT")
                .trxDate(OffsetDateTime.now())
                .amount(BigDecimal.TEN)
                .amountCurrency("AMOUNTCURRENCY")
                .mcc("MCC")
                .acquirerCode("ACQUIRERCODE")
                .acquirerId("ACQUIRERID")
                .idTrxAcquirer("IDTRXACQUIRER")
                .idTrxIssuer("IDTRXISSUER")
                .build();
        // When
        TransactionDTO result = mapper.apply(previewRequest);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(previewRequest.getTransactionId(), result.getId());
        Assertions.assertEquals(previewRequest.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertEquals(previewRequest.getAcquirerCode(), result.getAcquirerCode());
        Assertions.assertEquals(previewRequest.getTrxDate(), result.getTrxDate());
        Assertions.assertEquals(TransactionPreviewRequest2TransactionDTOMapper.getPaymentInstrument(previewRequest.getUserId()), result.getHpan());
        Assertions.assertEquals(operationType, result.getOperationType());
        Assertions.assertEquals(previewRequest.getIdTrxIssuer(), result.getIdTrxIssuer());
        Assertions.assertEquals(previewRequest.getAmount(), result.getAmount());
        Assertions.assertEquals(previewRequest.getAmountCurrency(), result.getAmountCurrency());
        Assertions.assertEquals(previewRequest.getMcc(), result.getMcc());
        Assertions.assertEquals(previewRequest.getAcquirerId(), result.getAcquirerId());
        Assertions.assertEquals(previewRequest.getMerchantId(), result.getMerchantId());
        Assertions.assertEquals(previewRequest.getMerchantFiscalCode(), result.getFiscalCode());
        Assertions.assertEquals(previewRequest.getVat(), result.getVat());
        Assertions.assertEquals(previewRequest.getUserId(), result.getUserId());
    }
}