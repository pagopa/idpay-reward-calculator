package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.mapper.RewardTransaction2PreviewResponseMapper;
import it.gov.pagopa.reward.dto.mapper.TransactionPreviewRequest2TransactionDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousRequest;
import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousResponse;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class RewardTrxSynchronousApiServiceImpl implements RewardTrxSynchronousApiService {
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;

    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final TransactionPreviewRequest2TransactionDTOMapper trxPreviewRequest2TransactionDtoMapper;
    private final RewardTransaction2PreviewResponseMapper rewardTransaction2PreviewResponseMapper;

    public RewardTrxSynchronousApiServiceImpl(OnboardedInitiativesService onboardedInitiativesService, InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService, UserInitiativeCountersRepository userInitiativeCountersRepository, TransactionPreviewRequest2TransactionDTOMapper trxPreviewRequest2TransactionDtoMapper, RewardTransaction2PreviewResponseMapper rewardTransaction2PreviewResponseMapper) {
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.trxPreviewRequest2TransactionDtoMapper = trxPreviewRequest2TransactionDtoMapper;
        this.rewardTransaction2PreviewResponseMapper = rewardTransaction2PreviewResponseMapper;
    }

    @Override
    public Mono<TransactionSynchronousResponse> postTransactionPreview(TransactionSynchronousRequest trxPreviewRequest, String initiativeId) { //TODO unificare TransactionPrevireRequest and Response (request Scarto)
        TransactionDTO trxDTO = trxPreviewRequest2TransactionDtoMapper.apply(trxPreviewRequest);

        return onboardedInitiativesService.isOnboarded(trxDTO.getHpan(),trxDTO.getTrxDate(), initiativeId)
                .flatMap(i -> {
                    if (i.equals(Boolean.TRUE)) {
                        return userInitiativeCountersRepository.findById(UserInitiativeCounters.buildId(trxDTO.getUserId(),initiativeId));
                    } else {
                        throw new IllegalArgumentException("User not onboarded to initiative %s".formatted(initiativeId));
                    }
                })
                .map(userInitiativeCounters -> {
                    UserInitiativeCountersWrapper counterWrapper = UserInitiativeCountersWrapper.builder()
                            .userId(userInitiativeCounters.getUserId())
                            .initiatives(Map.of(initiativeId, userInitiativeCounters))
                            .build();
                    return initiativesEvaluatorFacadeService.evaluateInitiativesBudgetAndRules(trxDTO, List.of(initiativeId), counterWrapper);
                })
                .map(p -> rewardTransaction2PreviewResponseMapper.apply(trxPreviewRequest.getTransactionId(),initiativeId, p.getSecond()));
    }
}
