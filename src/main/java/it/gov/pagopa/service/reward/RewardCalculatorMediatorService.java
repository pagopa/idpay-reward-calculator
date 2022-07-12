package it.gov.pagopa.service.reward;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.dto.TransactionDTO;
import reactor.core.publisher.Flux;

/**
 * This component will take a {@link TransactionDTO} and will calculate the {@link RewardTransactionDTO}
 * */
public interface RewardCalculatorMediatorService {
    Flux<RewardTransactionDTO> execute(Flux<TransactionDTO> transactionDTO);

}
