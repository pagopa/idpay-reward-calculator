package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HpanInitiativesServiceImpl implements HpanInitiativesService{
    private final AddHpanService addHpanService;
    private final DeleteHpanService deleteHpanService;

    public HpanInitiativesServiceImpl(AddHpanService addHpanService, DeleteHpanService deleteHpanService) {
        this.addHpanService = addHpanService;
        this.deleteHpanService = deleteHpanService;
    }

    @Override
    public HpanInitiatives evaluate(HpanInitiativeDTO hpanInitiativeDTO, HpanInitiatives hpanRetrieved) {
        return switch (hpanInitiativeDTO.getOperationType()) {
            case HpanInitiativeConstants.ADD_INSTRUMENT -> addHpanService.execute(hpanRetrieved,hpanInitiativeDTO);
            case HpanInitiativeConstants.DELETE_INSTRUMENT -> deleteHpanService.execute(hpanRetrieved, hpanInitiativeDTO);
            default -> invalidOperationType(hpanInitiativeDTO);
        };
    }
    private HpanInitiatives invalidOperationType(HpanInitiativeDTO hpanInitiativeDTO){
        log.error("Error in evaluate hpan update %s .Cause: operation type not valid".formatted(hpanInitiativeDTO.getHpan()));
        return null;
    }
}
