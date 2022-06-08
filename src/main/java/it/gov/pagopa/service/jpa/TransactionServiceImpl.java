package it.gov.pagopa.service.jpa;

import it.gov.pagopa.dto.TransactionPrizeDTO;
import it.gov.pagopa.model.TransactionPrize;
import it.gov.pagopa.repository.TransactionDAO;
import it.gov.pagopa.dto.mapper.TransactionPrizeMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TransactionServiceImpl implements TransactionService {

    private TransactionDAO trxDAO;

    private TransactionPrizeMapper trxPrizeMapper;

    public TransactionServiceImpl(TransactionDAO trxDAO, TransactionPrizeMapper trxPrizeMapper){
        this.trxDAO = trxDAO;
        this.trxPrizeMapper = trxPrizeMapper;
    }

    @Override
    public Mono<TransactionPrizeDTO> getById(String idTrxAcquirer, String acquirerCode, String trxDate) {
        Mono<TransactionPrize> trxRetrieved = trxDAO.findById(idTrxAcquirer, acquirerCode,trxDate);
        return trxRetrieved.map(trxPrizeMapper::toDTO);
    }


    @Override
    public Mono<TransactionPrize> save(TransactionPrize transaction) {
        return trxDAO.save(transaction);
    }


}
