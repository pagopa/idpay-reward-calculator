package it.gov.pagopa.service.reward;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class RewardCalculatorMediatorServiceImpl implements RewardCalculatorMediatorService{

    private final TransactionFilterService transactionFilterService;
    private final InitiativesService initiativesService;
    private final RuleEngineService ruleEngineService;

    public RewardCalculatorMediatorServiceImpl(TransactionFilterService transactionFilterService, InitiativesService initiativesService, RuleEngineService ruleEngineService) {
        this.transactionFilterService = transactionFilterService;
        this.initiativesService = initiativesService;
        this.ruleEngineService = ruleEngineService;
    }

    @Override
    public Flux<RewardTransactionDTO> execute(Flux<TransactionDTO> transactionDTOFlux) {

        return transactionDTOFlux
                .filter(transactionFilterService::filter)
                .mapNotNull(t -> ruleEngineService.applyRules(t, initiativesService.getInitiatives(t.getHpan(), t.getTrxDate())));
    }

}
