package it.gov.pagopa.reward.service.recess;

import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.service.lookup.HpanInitiativesService;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class InstrumentApiServiceImpl implements InstrumentApiService {

    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final HpanInitiativesService hpanInitiativesService;

    public InstrumentApiServiceImpl(HpanInitiativesRepository hpanInitiativesRepository, HpanInitiativesService hpanInitiativesService) {
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.hpanInitiativesService = hpanInitiativesService;
    }

    @Override
    public Mono<Void> cancelInstruments(String userId, String initiativeId) {
        LocalDateTime evaluationDate = LocalDateTime.now();

        return hpanInitiativesRepository.retrieveHpanByUserIdAndInitiativeId(userId, initiativeId)
                .flatMap(hpanInitiatives -> cancelEvaluate(userId, initiativeId, hpanInitiatives, evaluationDate))
                .collectList()
                .then();
    }

    private Mono<String> cancelEvaluate(String userId, String initiativeId, HpanInitiatives hpanInitiatives, LocalDateTime evaluationDate) {
        return Mono.just(getHpanUpdateEvaluateRequest(userId, initiativeId, hpanInitiatives, evaluationDate, HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT))
                .flatMap(hpanUpdateEvaluateDTO -> evaluateAndSave(hpanUpdateEvaluateDTO, hpanInitiatives));
    }

    @NotNull
    private Mono<String> evaluateAndSave(HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO, HpanInitiatives hpanInitiatives) {
        return Mono.just(hpanInitiatives)
                .mapNotNull(hi -> hpanInitiativesService.evaluate(hpanUpdateEvaluateDTO, hi))
                .flatMap(oi -> hpanInitiativesRepository.setInitiative(hpanUpdateEvaluateDTO.getHpan(), oi))
                .map(ur -> hpanUpdateEvaluateDTO.getHpan());
    }

    private HpanUpdateEvaluateDTO getHpanUpdateEvaluateRequest(String userId, String initiativeId, HpanInitiatives hpanInitiatives, LocalDateTime evaluationDate, String operationType) {
        return HpanUpdateEvaluateDTO.builder()
                .userId(userId)
                .initiativeId(initiativeId)
                .hpan(hpanInitiatives.getHpan())
                .operationType(operationType)
                .evaluationDate(evaluationDate)
                .build();
    }
}
