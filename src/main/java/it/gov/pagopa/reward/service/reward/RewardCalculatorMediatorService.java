package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component will take a {@link TransactionDTO} and will calculate the {@link RewardTransactionDTO}
 * */
public interface RewardCalculatorMediatorService {
    void execute(Flux<Message<String>> transactionDTO);

}
