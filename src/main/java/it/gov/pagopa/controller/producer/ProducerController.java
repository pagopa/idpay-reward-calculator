package it.gov.pagopa.controller.producer;

import it.gov.pagopa.dto.TransactionDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Interfaccia dedicata al Producer
 */
@RequestMapping("/transactions")
public interface ProducerController {

    /**
     * API di invio del dato (Transazione)
     *
     * @param transaction
     */
    @PostMapping
    void sendTransaction(@RequestBody TransactionDTO transaction);
}
