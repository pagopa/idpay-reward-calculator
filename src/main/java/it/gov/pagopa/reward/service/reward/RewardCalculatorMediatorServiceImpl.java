package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService{

    private final TransactionFilterService transactionFilterService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final RuleEngineService ruleEngineService;

    public RewardCalculatorMediatorServiceImpl(TransactionFilterService transactionFilterService, OnboardedInitiativesService onboardedInitiativesService, RuleEngineService ruleEngineService) {
        this.transactionFilterService = transactionFilterService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.ruleEngineService = ruleEngineService;
    }

    @Override
    public Flux<RewardTransactionDTO> execute(Flux<TransactionDTO> transactionDTOFlux) {

        return transactionDTOFlux
                .filter(transactionFilterService::filter)
                .mapNotNull(t -> ruleEngineService.applyRules(t, onboardedInitiativesService.getInitiatives(t.getHpan(), t.getTrxDate())));
    }

}
