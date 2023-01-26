package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;

/** It will re-publish a {@link TransactionDTO} in the input topic, clearing its state before */
public interface TrxRePublisherService {
    boolean notify(TransactionDTO reward);
}
