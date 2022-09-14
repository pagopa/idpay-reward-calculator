package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Transaction2RewardTransactionMapper implements Function<TransactionDTO, RewardTransactionDTO> {
    @Override
    public RewardTransactionDTO apply(TransactionDTO trx) {
        RewardTransactionDTO out = null;

        if (trx != null) {
            out = new RewardTransactionDTO();
            copyFields(trx, out);

            /*out.setStatus(
                    CollectionUtils.isEmpty(trx.getRejectionReasons())
                            ? RewardConstants.REWARD_STATE_REWARDED
                            : RewardConstants.REWARD_STATE_REJECTED
            );*/
        }

        return out;
    }

    public static void copyFields(TransactionDTO src, TransactionDTO dest){
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
//        dest.setRejectionReasons(src.getRejectionReasons());
        dest.setUserId(src.getUserId());
//        dest.setEffectiveAmount(src.getEffectiveAmount());
//        dest.setTrxChargeDate(src.getTrxChargeDate());
//        dest.setRefundInfo(src.getRefundInfo());
//        dest.setOperationTypeTranscoded(src.getOperationTypeTranscoded());
    }
}
