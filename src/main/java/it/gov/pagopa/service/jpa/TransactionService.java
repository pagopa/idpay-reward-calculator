package it.gov.pagopa.service.jpa;

import it.gov.pagopa.dto.TransactionPrizeDTO;
import it.gov.pagopa.model.TransactionPrize;
import reactor.core.publisher.Mono;

public interface TransactionService {
    Mono<TransactionPrizeDTO> getById(String idTrxAcquirer, String acquirerCode, String trxDate);

    Mono<TransactionPrize> save(TransactionPrize transaction);
}
