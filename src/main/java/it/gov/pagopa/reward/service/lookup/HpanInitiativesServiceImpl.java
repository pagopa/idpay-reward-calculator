package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.service.lookup.ops.AddHpanService;
import it.gov.pagopa.reward.service.lookup.ops.DeleteHpanService;
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
    public OnboardedInitiative evaluate(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO, HpanInitiatives hpanRetrieved) {
        return switch (hpanUpdateEvaluateDTO.getOperationType()) {
            case HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT -> addHpanService.execute(hpanRetrieved, hpanUpdateEvaluateDTO);
            case HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT -> deleteHpanService.execute(hpanRetrieved, hpanUpdateEvaluateDTO);
            default -> invalidOperationType(hpanUpdateEvaluateDTO);
        };
    }
    private OnboardedInitiative invalidOperationType(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO){
        log.error("Error in evaluate hpan update %s .Cause: operation type not valid".formatted(hpanUpdateEvaluateDTO.getHpan()));
        return null;
    }
}
