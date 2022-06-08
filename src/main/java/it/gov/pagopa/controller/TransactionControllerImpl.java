package it.gov.pagopa.controller;

import it.gov.pagopa.dto.TransactionPrizeDTO;
import it.gov.pagopa.service.jpa.TransactionService;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TransactionControllerImpl implements TransactionController {

    private final TransactionService transactionService;

    public TransactionControllerImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    @Override
    public Mono<TransactionPrizeDTO> getTransactionById(String idTrxAcquirer, String acquirerCode, String trxDate) {
        return transactionService.getById(idTrxAcquirer, acquirerCode, trxDate);
    }

}
