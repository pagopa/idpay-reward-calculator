package it.gov.pagopa.service;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.mapper.TransactionMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService{

    private final TransactionFilterService transactionFilterService;
    private final InitiativesService initiativesService;
    private final RuleEngineService ruleEngineService;
    private final TransactionMapper transactionMapper;

    public RewardCalculatorMediatorServiceImpl(TransactionFilterService transactionFilterService, InitiativesService initiativesService, RuleEngineService ruleEngineService, TransactionMapper transactionMapper) {
        this.transactionFilterService = transactionFilterService;
        this.initiativesService = initiativesService;
        this.ruleEngineService = ruleEngineService;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public RewardTransactionDTO execute(TransactionDTO transactionDTO) {
        if(transactionFilterService.filter(transactionDTO)){
            List<String> initiatives = initiativesService.getInitiatives(transactionDTO.getHpan(), transactionDTO.getTrxDate());
            if(CollectionUtils.isNotEmpty(initiatives)){
                return ruleEngineService.applyRules(transactionDTO, initiatives);
            }else{
                log.debug("Not initiatives found for the transaction"); //TODO log some identifier of the transaction
                //TODO set rejectionReason, no rewards and return it
                RewardTransactionDTO rewardTransactionDTO = transactionMapper.mapDTO(transactionDTO);
                rewardTransactionDTO.setStatus("REJECTED");
                rewardTransactionDTO.setRejectionReason("Not initiatives found");
                return rewardTransactionDTO;
            }
        }else {
            log.debug("The transaction has been rejected"); //TODO log some identifier of the transaction
            RewardTransactionDTO rewardTransactionDTO = transactionMapper.mapDTO(transactionDTO);
            rewardTransactionDTO.setStatus("REJECTED");
            rewardTransactionDTO.setRejectionReason("MCC no valid");
            return rewardTransactionDTO;
        }
    }
}
