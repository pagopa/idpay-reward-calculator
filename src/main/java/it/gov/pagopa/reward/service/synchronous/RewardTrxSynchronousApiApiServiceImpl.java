package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.mapper.RewardTransaction2PreviewResponseMapper;
import it.gov.pagopa.reward.dto.mapper.TransactionPreviewRequest2TransactionDTOMapper;
import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewRequest;
import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewResponse;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.exception.ClientExceptionWithBody;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RewardTrxSynchronousApiApiServiceImpl implements RewardTrxSynchronousApiService {
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;

    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final TransactionPreviewRequest2TransactionDTOMapper trxPreviewRequest2TransactionDtoMapper;
    private final RewardTransaction2PreviewResponseMapper rewardTransaction2PreviewResponseMapper;

    public RewardTrxSynchronousApiApiServiceImpl(OnboardedInitiativesService onboardedInitiativesService, InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService, UserInitiativeCountersRepository userInitiativeCountersRepository, TransactionPreviewRequest2TransactionDTOMapper trxPreviewRequest2TransactionDtoMapper, RewardTransaction2PreviewResponseMapper rewardTransaction2PreviewResponseMapper) {
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.trxPreviewRequest2TransactionDtoMapper = trxPreviewRequest2TransactionDtoMapper;
        this.rewardTransaction2PreviewResponseMapper = rewardTransaction2PreviewResponseMapper;
    }

    @Override
    public Mono<TransactionPreviewResponse> postTransactionPreview(TransactionPreviewRequest trxPreviewRequest, String initiativeId) {
        TransactionDTO trxDTO = trxPreviewRequest2TransactionDtoMapper.apply(trxPreviewRequest);

        return onboardedInitiativesService.isOnboarded(trxDTO.getHpan(), initiativeId)
                .flatMap(i -> userInitiativeCountersRepository.findById(trxPreviewRequest.getUserId())) //TODO counter Logics
                .map(userCounters -> initiativesEvaluatorFacadeService.evaluateInitiativesBudgetAndRules(trxDTO, List.of(initiativeId), userCounters))
                .map(p -> rewardTransaction2PreviewResponseMapper.apply(trxPreviewRequest.getTransactionId(),initiativeId, p.getSecond()))
                .doOnError(e -> {
                    if(e instanceof  IllegalArgumentException){
                        throw new ClientExceptionWithBody(HttpStatus.FORBIDDEN,"Error",  e.getMessage());
                    }
                });
    }
}
