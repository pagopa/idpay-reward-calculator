package it.gov.pagopa.controller;

import it.gov.pagopa.dto.TransactionPrizeDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/**
 * Interfaccia dedicata alle Transazioni
 */
@RequestMapping("/transactions")
public interface TransactionController {

    /**
     * API di richiesta del dato (Transazione)
     *
     * @param idTrxAcquirer
     * @param acquirerCode
     * @param trxDate
     * @return
     */
    @GetMapping
    Mono<TransactionPrizeDTO> getTransactionById(
            @RequestParam("idTrxAcquirer") String idTrxAcquirer,
            @RequestParam("acquirerCode") String acquirerCode,
            @RequestParam("trxDate") String trxDate
    );

}
