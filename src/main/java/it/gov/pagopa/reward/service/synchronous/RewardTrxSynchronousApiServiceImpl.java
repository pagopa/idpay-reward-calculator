package it.gov.pagopa.reward.service.synchronous;

import it.gov.pagopa.reward.dto.mapper.RewardTransaction2SynchronousTransactionRequestDTOMapper;
import it.gov.pagopa.reward.dto.mapper.SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.exception.TransactionSynchronousException;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.OnboardedInitiativesService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RewardTrxSynchronousApiServiceImpl implements RewardTrxSynchronousApiService {
    private final RewardContextHolderService rewardContextHolderService;
    private final OnboardedInitiativesService onboardedInitiativesService;
    private final InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService;

    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper trxPreviewRequest2TransactionDtoMapper;
    private final RewardTransaction2SynchronousTransactionRequestDTOMapper rewardTransaction2SynchronousTransactionRequestDTOMapper;

    public RewardTrxSynchronousApiServiceImpl(RewardContextHolderService rewardContextHolderService, OnboardedInitiativesService onboardedInitiativesService, InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeService, UserInitiativeCountersRepository userInitiativeCountersRepository, SynchronousTransactionRequestDTOt2TrxDtoOrResponseMapper trxPreviewRequest2TransactionDtoMapper, RewardTransaction2SynchronousTransactionRequestDTOMapper rewardTransaction2SynchronousTransactionRequestDTOMapper) {
        this.rewardContextHolderService = rewardContextHolderService;
        this.onboardedInitiativesService = onboardedInitiativesService;
        this.initiativesEvaluatorFacadeService = initiativesEvaluatorFacadeService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.trxPreviewRequest2TransactionDtoMapper = trxPreviewRequest2TransactionDtoMapper;
        this.rewardTransaction2SynchronousTransactionRequestDTOMapper = rewardTransaction2SynchronousTransactionRequestDTOMapper;
    }

    @Override
    public Mono<SynchronousTransactionResponseDTO> previewTransaction(SynchronousTransactionRequestDTO trxPreviewRequest, String initiativeId) {
        log.trace("[REWARD] Starting reward preview calculation for transaction {}", trxPreviewRequest.getTransactionId());
        TransactionDTO trxDTO = trxPreviewRequest2TransactionDtoMapper.apply(trxPreviewRequest);

        return checkInitiative(trxPreviewRequest, initiativeId)
                .flatMap(b -> checkOnboarded(trxPreviewRequest, trxDTO, initiativeId))
                .flatMap(b -> userInitiativeCountersRepository.findById(UserInitiativeCounters.buildId(trxDTO.getUserId(),initiativeId)))
                .map(userInitiativeCounters -> {
                    UserInitiativeCountersWrapper counterWrapper = UserInitiativeCountersWrapper.builder()
                            .userId(userInitiativeCounters.getUserId())
                            .initiatives(Map.of(initiativeId, userInitiativeCounters))
                            .build();
                    return initiativesEvaluatorFacadeService.evaluateInitiativesBudgetAndRules(trxDTO, List.of(initiativeId), counterWrapper);
                })
                .map(p -> rewardTransaction2SynchronousTransactionRequestDTOMapper.apply(trxPreviewRequest.getTransactionId(),initiativeId, p.getSecond()));
    }

    private Mono<Boolean> checkInitiative(SynchronousTransactionRequestDTO request, String initiativeId){
        return rewardContextHolderService.getInitiativeConfig(initiativeId).hasElement()
                .map(b -> checkingResult(b, request, initiativeId, RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND));
    }
    private Mono<Boolean> checkOnboarded(SynchronousTransactionRequestDTO request, TransactionDTO trx, String initiativeId){
        return onboardedInitiativesService.isOnboarded(trx.getHpan(),trx.getTrxDate(), initiativeId)
                .map(b -> checkingResult(b, request, initiativeId, RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));
    }

    private Boolean checkingResult(Boolean b, SynchronousTransactionRequestDTO request, String initiativeId, String trxRejectionReasonNoInitiative) {
        if ( b.equals(Boolean.TRUE)){
            return Boolean.TRUE;
        } else {
            throw new TransactionSynchronousException(trxPreviewRequest2TransactionDtoMapper.apply(request, initiativeId, List.of(trxRejectionReasonNoInitiative)));
        }
    }
}
