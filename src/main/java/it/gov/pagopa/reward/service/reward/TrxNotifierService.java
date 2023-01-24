package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;

public interface TrxNotifierService {
    boolean notify(TransactionDTO reward);
}
