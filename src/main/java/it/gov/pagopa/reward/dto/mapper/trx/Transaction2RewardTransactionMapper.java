package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.function.Function;

@Service
public class Transaction2RewardTransactionMapper implements Function<TransactionDTO, RewardTransactionDTO> {
    @Override
    public RewardTransactionDTO apply(TransactionDTO trx) {
        RewardTransactionDTO out = null;

        if (trx != null) {
            out = new RewardTransactionDTO();
            copyFields(trx, out);

            out.setStatus(
                    CollectionUtils.isEmpty(trx.getRejectionReasons())
                            ? RewardConstants.REWARD_STATE_REWARDED
                            : RewardConstants.REWARD_STATE_REJECTED
            );

            out.setRewards(new HashMap<>());
            out.setInitiativeRejectionReasons(new HashMap<>());
        }

        return out;
    }

    public static void copyFields(TransactionDTO src, TransactionDTO dest){
        dest.setId(src.getId());
        dest.setIdTrxAcquirer(src.getIdTrxAcquirer());
        dest.setAcquirerCode(src.getAcquirerCode());
        dest.setTrxDate(src.getTrxDate());
        dest.setHpan(src.getHpan());
        dest.setCircuitType(src.getCircuitType());
        dest.setOperationType(src.getOperationType());
        dest.setIdTrxIssuer(src.getIdTrxIssuer());
        dest.setCorrelationId(src.getCorrelationId());
        dest.setAmount(src.getAmount());
        dest.setAmountCurrency(src.getAmountCurrency());
        dest.setMcc(src.getMcc());
        dest.setAcquirerId(src.getAcquirerId());
        dest.setMerchantId(src.getMerchantId());
        dest.setTerminalId(src.getTerminalId());
        dest.setBin(src.getBin());
        dest.setSenderCode(src.getSenderCode());
        dest.setFiscalCode(src.getFiscalCode());
        dest.setVat(src.getVat());
        dest.setPosType(src.getPosType());
        dest.setPar(src.getPar());
        dest.setRejectionReasons(src.getRejectionReasons());
        dest.setUserId(src.getUserId());
        dest.setEffectiveAmountCents(src.getEffectiveAmountCents());
        dest.setAmountCents(src.getAmountCents());
        dest.setTrxChargeDate(src.getTrxChargeDate());
        dest.setRefundInfo(src.getRefundInfo());
        dest.setOperationTypeTranscoded(src.getOperationTypeTranscoded());
        dest.setUserId(src.getUserId());
        dest.setMaskedPan(src.getMaskedPan());
        dest.setBrandLogo(src.getBrandLogo());
        dest.setBrand(src.getBrand());
        dest.setChannel(src.getChannel());
        dest.setRuleEngineTopicPartition(src.getRuleEngineTopicPartition());
        dest.setRuleEngineTopicOffset(src.getRuleEngineTopicOffset());
    }
}
