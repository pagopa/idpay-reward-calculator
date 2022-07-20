package it.gov.pagopa.reward.service.reward.filter;

import it.gov.pagopa.reward.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@Order(0)
public class MccTransactionFilter implements TransactionFilter{

    private final Set<String> mccExcluded;

    public MccTransactionFilter(@Value("${app.filter.mccExcluded}") Set<String> mccExcluded) {
        this.mccExcluded = mccExcluded;
    }

    @Override
    public boolean test(TransactionDTO transactionDTO) {
        boolean excluded = !mccExcluded.contains(transactionDTO.getMcc());
        log.info(String.format("Transaction mcc: %s - Is accepted: %b",transactionDTO.getMcc(),excluded));
        return excluded;
    }
}