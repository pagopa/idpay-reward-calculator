package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewRequest;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class TransactionPreviewRequest2TransactionDTOMapper implements Function<TransactionPreviewRequest, TransactionDTO> {
    private final String chargeOperation;

    private static final String PREFIX_PAYMENT_INSTRUMENT="IDPAY_%s";

    public TransactionPreviewRequest2TransactionDTOMapper(@Value("${app.operationType.charge}")String chargeOperation) {
        this.chargeOperation = chargeOperation;
    }

    @Override
    public TransactionDTO apply(TransactionPreviewRequest trx) {
        TransactionDTO out = new TransactionDTO();
        out.setId(trx.getTransactionId());
        out.setIdTrxAcquirer(trx.getIdTrxAcquirer());
        out.setAcquirerCode(trx.getAcquirerCode());
        out.setTrxDate(trx.getTrxDate());
        if (trx.getHpan() == null) {
            out.setHpan(getPaymentInstrument(trx.getUserId()));
        } else {
            out.setHpan(trx.getHpan());
        }
        out.setOperationType(chargeOperation);
        out.setIdTrxIssuer(trx.getIdTrxIssuer());
        out.setAmount(trx.getAmount());
        out.setAmountCurrency(trx.getAmountCurrency());
        out.setMcc(trx.getMcc());
        out.setAcquirerId(trx.getAcquirerId());
        out.setMerchantId(trx.getMerchantId());
        out.setFiscalCode(trx.getMerchantFiscalCode());
        out.setVat(trx.getVat());
        out.setUserId(trx.getUserId());

        return out;
    }

    public static String getPaymentInstrument(String userId){
        return PREFIX_PAYMENT_INSTRUMENT.formatted(userId);
    }
}
