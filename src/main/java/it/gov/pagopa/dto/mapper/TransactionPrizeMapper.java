package it.gov.pagopa.dto.mapper;

import it.gov.pagopa.dto.TransactionPrizeDTO;
import it.gov.pagopa.model.TransactionPrize;
import org.springframework.stereotype.Service;

@Service
public class TransactionPrizeMapper {
    public TransactionPrizeDTO toDTO (TransactionPrize trxPrize) {
        TransactionPrizeDTO trxDto = null;

        if (trxPrize != null){
            trxDto = TransactionPrizeDTO.builder().build();
            trxDto.setIdTrxAcquirer(trxPrize.getIdTrxAcquirer());
            trxDto.setAcquirerCode(trxPrize.getAcquirerCode());
            trxDto.setTrxDate(trxPrize.getTrxDate());
            trxDto.setHpan(trxPrize.getHpan());
            trxDto.setOperationType(trxPrize.getOperationType());
            trxDto.setIdTrxIssuer(trxPrize.getIdTrxIssuer());
            trxDto.setCorrelationId(trxPrize.getCorrelationId());
            trxDto.setAmount(trxPrize.getAmount());
            trxDto.setAmountCurrency(trxPrize.getAmountCurrency());
            trxDto.setMcc(trxPrize.getMcc());
            trxDto.setAcquirerId(trxPrize.getAcquirerId());
            trxDto.setMerchantId(trxPrize.getMerchantId());
            trxDto.setTerminalId(trxPrize.getTerminalId());
            trxDto.setBin(trxPrize.getBin());
            trxDto.setPrize(trxPrize.getPrize());
        }

        return trxDto;

    }
}
