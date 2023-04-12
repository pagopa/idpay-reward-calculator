package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.utils.RewardConstants;
import it.gov.pagopa.reward.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper {
    private final String chargeOperation;

    private static final String PREFIX_PAYMENT_INSTRUMENT="IDPAY_%s";

    public SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper(@Value("${app.operationType.charge}")String chargeOperation) {
        this.chargeOperation = chargeOperation;
    }

    public TransactionDTO apply(SynchronousTransactionRequestDTO trx) {
        TransactionDTO out = new TransactionDTO();
        BigDecimal amount = Utils.centsToEuro(trx.getAmountCents());

        out.setId(trx.getTransactionId());
        out.setIdTrxAcquirer(trx.getIdTrxAcquirer());
        out.setAcquirerCode(trx.getAcquirerCode());
        out.setTrxDate(trx.getTrxDate());
        out.setHpan(trx.getHpan() == null ? getPaymentInstrument(trx.getUserId()) : trx.getHpan());
        out.setOperationType(chargeOperation);
        out.setIdTrxIssuer(trx.getIdTrxIssuer());
        out.setCorrelationId(trx.getCorrelationId());
        out.setAmount(amount);
        out.setAmountCurrency(trx.getAmountCurrency());
        out.setMcc(trx.getMcc());
        out.setAcquirerId(trx.getAcquirerId());
        out.setMerchantId(trx.getMerchantId());
        out.setFiscalCode(trx.getMerchantFiscalCode());
        out.setVat(trx.getVat());
        out.setOperationTypeTranscoded(OperationType.CHARGE);
        out.setAmountCents(trx.getAmountCents());
        out.setEffectiveAmount(amount);
        out.setTrxChargeDate(trx.getTrxChargeDate());
        out.setUserId(trx.getUserId());

        return out;
    }

    public SynchronousTransactionResponseDTO apply(SynchronousTransactionRequestDTO request, String initiativeId, List<String> discardCause){
        SynchronousTransactionResponseDTO out = new SynchronousTransactionResponseDTO();
        out.setTransactionId(request.getTransactionId());
        out.setInitiativeId(initiativeId);
        out.setUserId(request.getUserId());
        out.setStatus(RewardConstants.REWARD_STATE_REJECTED);
        out.setRejectionReasons(discardCause);
        return out;
    }

    public static String getPaymentInstrument(String userId){
        return PREFIX_PAYMENT_INSTRUMENT.formatted(userId);
    }
}
