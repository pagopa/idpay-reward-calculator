package it.gov.pagopa.repository;

import it.gov.pagopa.model.TransactionPrize;
import it.gov.pagopa.model.TransactionPrizeId;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TransactionDAO extends ReactiveCrudRepository<TransactionPrize, TransactionPrizeId> {
    @Query("select * from transaction where id_trx_acquirer_s=:idTrxAcquirer and acquirer_c=:acquirerCode and trx_date_s=:trxDate")
    Mono<TransactionPrize> findById(@Param("idTrxAcquirer") String idTrxAcquirer,
                                    @Param("acquirerCode") String acquirerCode,
                                    @Param("trxDate") String trxDate);

}
