package it.gov.pagopa.reward.controller.producer;

import it.gov.pagopa.reward.dto.TransactionDTO;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Sinks;

@RestController
public class ProducerControllerImpl implements ProducerController {
    private final Sinks.Many<TransactionDTO> many;

    public ProducerControllerImpl(Sinks.Many<TransactionDTO> many) {
        this.many = many;
    }

    @Override
    public void sendTransaction(TransactionDTO transaction) {
        many.emitNext(transaction, Sinks.EmitFailureHandler.FAIL_FAST);
    }

}
