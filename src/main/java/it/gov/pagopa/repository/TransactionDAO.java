package it.gov.pagopa.repository;

import it.gov.pagopa.model.TransactionPrize;
import it.gov.pagopa.model.TransactionPrizeId;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TransactionDAO extends ReactiveCrudRepository<TransactionPrize, TransactionPrizeId> {
    Mono<TransactionPrize> findByIdTrxAcquirerAndAcquirerCodeAndTrxDate(String idTrxAcquirer, String acquirerCode, String trxDate);

}
