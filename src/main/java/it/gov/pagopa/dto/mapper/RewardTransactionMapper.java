package it.gov.pagopa.dto.mapper;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.model.RewardTransaction;
import org.springframework.stereotype.Service;

@Service
public class RewardTransactionMapper {
    public RewardTransactionDTO toDTO (RewardTransaction rewardTrx) {
        RewardTransactionDTO trxDto = null;

        if (rewardTrx != null){
            trxDto = RewardTransactionDTO.builder().build();
            trxDto.setIdTrxAcquirer(rewardTrx.getIdTrxAcquirer());
            trxDto.setAcquirerCode(rewardTrx.getAcquirerCode());
            trxDto.setTrxDate(rewardTrx.getTrxDate());
            trxDto.setHpan(rewardTrx.getHpan());
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
            trxDto.setReward(rewardTrx.getReward());
        }

        return trxDto;

    }
}
