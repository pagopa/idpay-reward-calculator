package it.gov.pagopa.reward.service.recess;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.service.lookup.HpanInitiativeMediatorService;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class InstrumentApiServiceImpl implements InstrumentApiService {

    private final HpanInitiativeMediatorService hpanInitiativeMediatorService;

    public InstrumentApiServiceImpl(HpanInitiativeMediatorService hpanInitiativeMediatorService) {
        this.hpanInitiativeMediatorService = hpanInitiativeMediatorService;
    }

    @Override
    public Mono<List<String>> cancelInstruments(HpanInitiativeBulkDTO hpanInitiativeBulkDTO) {
        if(!HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT.equals(hpanInitiativeBulkDTO.getOperationType())){
            return Mono.error(new IllegalArgumentException("[SYNC_CANCEL_INSTRUMENTS] Operation type not valid"));
        }
        return hpanInitiativeMediatorService.evaluate(hpanInitiativeBulkDTO, LocalDateTime.now())
                .collectList();
    }
}
