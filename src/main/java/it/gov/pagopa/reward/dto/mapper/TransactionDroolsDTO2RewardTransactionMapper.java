package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.function.Function;

@Service
public class TransactionDroolsDTO2RewardTransactionMapper implements Function<TransactionDroolsDTO, RewardTransactionDTO> {
    @Override
    public RewardTransactionDTO apply(TransactionDroolsDTO rewardTrx) {
        RewardTransactionDTO trxDto = null;

        if (rewardTrx != null) {
            trxDto = new RewardTransactionDTO();
            trxDto.setIdTrxAcquirer(rewardTrx.getIdTrxAcquirer());
            trxDto.setAcquirerCode(rewardTrx.getAcquirerCode());
            trxDto.setTrxDate(rewardTrx.getTrxDate());
            trxDto.setHpan(rewardTrx.getHpan());
            trxDto.setCircuitType(rewardTrx.getCircuitType());
            trxDto.setOperationType(rewardTrx.getOperationType());
            trxDto.setIdTrxIssuer(rewardTrx.getIdTrxIssuer());
            trxDto.setCorrelationId(rewardTrx.getCorrelationId());
            trxDto.setAmount(rewardTrx.getAmount());
            trxDto.setAmountCurrency(rewardTrx.getAmountCurrency());
            trxDto.setMcc(rewardTrx.getMcc());
            trxDto.setAcquirerId(rewardTrx.getAcquirerId());
            trxDto.setMerchantId(rewardTrx.getMerchantId());
            trxDto.setTerminalId(rewardTrx.getTerminalId());
            trxDto.setBin(rewardTrx.getBin());
            trxDto.setSenderCode(rewardTrx.getSenderCode());
            trxDto.setFiscalCode(rewardTrx.getFiscalCode());
            trxDto.setVat(rewardTrx.getVat());
            trxDto.setPosType(rewardTrx.getPosType());
            trxDto.setPar(rewardTrx.getPar());
            trxDto.setRejectionReasons(rewardTrx.getRejectionReasons());
            trxDto.setInitiativeRejectionReasons(rewardTrx.getInitiativeRejectionReasons());
            trxDto.setRewards(rewardTrx.getRewards());

            trxDto.setStatus(
                    CollectionUtils.isEmpty(rewardTrx.getRejectionReasons()) &&
                            rewardTrx.getRewards().values().stream().anyMatch(r->r.getAccruedReward().compareTo(BigDecimal.ZERO)!=0)
                            ? "REWARDED"
                            : "REJECTED"
            );

            trxDto.setUserId(rewardTrx.getUserId());
        }

        return trxDto;
    }
}
