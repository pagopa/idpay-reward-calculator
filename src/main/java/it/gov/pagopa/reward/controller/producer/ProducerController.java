package it.gov.pagopa.reward.controller.producer;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Interfaccia dedicata al Producer
 */
@RequestMapping("/idpay/transactions")
public interface ProducerController {

    /**
     * API di invio del dato (Transazione)
     */
    @PostMapping
    void sendTransaction(@RequestBody TransactionDTO transaction);
}
