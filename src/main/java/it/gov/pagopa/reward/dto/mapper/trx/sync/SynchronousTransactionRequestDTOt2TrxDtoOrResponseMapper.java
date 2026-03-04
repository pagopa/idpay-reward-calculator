package it.gov.pagopa.reward.dto.mapper.trx.sync;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper {
    private final String chargeOperation;

    public SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper(@Value("${app.operationType.charge}") String chargeOperation) {
        this.chargeOperation = chargeOperation;
    }

    public TransactionDTO apply(SynchronousTransactionRequestDTO trx) {
        TransactionDTO out = new TransactionDTO();
        BigDecimal amount = CommonUtilities.centsToEuro(trx.getAmountCents());

        out.setId(trx.getTransactionId());
        out.setIdTrxAcquirer(trx.getIdTrxAcquirer());
        out.setAcquirerCode(trx.getAcquirerCode());
        out.setTrxDate(trx.getTrxDate());
        out.setOperationType(chargeOperation);
        out.setIdTrxIssuer(trx.getIdTrxIssuer());
        out.setCorrelationId(trx.getTransactionId());
        out.setAmount(amount);
        out.setAmountCurrency(trx.getAmountCurrency());
        out.setMcc(trx.getMcc());
        out.setAcquirerId(trx.getAcquirerId());
        out.setMerchantId(trx.getMerchantId());
        out.setFiscalCode(trx.getMerchantFiscalCode());
        out.setVat(trx.getVat());
        out.setOperationTypeTranscoded(OperationType.CHARGE);
        out.setAmountCents(trx.getAmountCents());
        out.setEffectiveAmountCents(trx.getAmountCents());
        out.setTrxChargeDate(trx.getTrxChargeDate());
        out.setUserId(trx.getUserId());
        out.setChannel(trx.getChannel());
        out.setVoucherAmountCents(trx.getVoucherAmountCents());

        return out;
    }

    public SynchronousTransactionResponseDTO apply(SynchronousTransactionRequestDTO request, String initiativeId, List<String> discardCause){
        SynchronousTransactionResponseDTO out = new SynchronousTransactionResponseDTO();
        out.setTransactionId(request.getTransactionId());
        out.setChannel(request.getChannel());
        out.setInitiativeId(initiativeId);
        out.setUserId(request.getUserId());
        out.setOperationType(OperationType.CHARGE);
        out.setAmountCents(request.getAmountCents());
        out.setAmount(CommonUtilities.centsToEuro(request.getAmountCents()));
        out.setEffectiveAmountCents(request.getAmountCents());
        out.setStatus(RewardConstants.REWARD_STATE_REJECTED);
        out.setRejectionReasons(discardCause);
        out.setRewards(java.util.Collections.emptyMap());
        out.setTrxNumber(0L);
        out.setTotalAmountCents(0L);
        out.setTotalRewardCents(0L);
        return out;
    }
}
