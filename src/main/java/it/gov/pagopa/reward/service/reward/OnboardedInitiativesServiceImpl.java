package it.gov.pagopa.reward.service.reward;

import static it.gov.pagopa.reward.utils.Utils.sanitizeString;

import it.gov.pagopa.reward.connector.rest.onboarding.OnboardingWorkflowConnector;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.BaseOnboardingInfo;
import it.gov.pagopa.reward.model.OnboardingInfo;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OnboardedInitiativesServiceImpl implements OnboardedInitiativesService {

    private final RewardContextHolderService rewardContextHolderService;
    private final OnboardingWorkflowConnector onboardingWorkflowConnector;

    public OnboardedInitiativesServiceImpl(
            RewardContextHolderService rewardContextHolderService,
            OnboardingWorkflowConnector onboardingWorkflowConnector) {
        this.rewardContextHolderService = rewardContextHolderService;
        this.onboardingWorkflowConnector = onboardingWorkflowConnector;
    }

    @Override
    public Mono<Pair<InitiativeConfig, OnboardingInfo>> isOnboarded(String userId, OffsetDateTime trxDate, String initiativeId) {
        var safeUserId = sanitizeString(userId);
        var safeInitiativeId = sanitizeString(initiativeId);
        log.debug("[REWARD][IS_ONBOARDED] Checking onboarding status via REST for userId: {}, initiativeId: {}", safeUserId, safeInitiativeId);
        return onboardingWorkflowConnector.getOnboardingStatus(safeUserId, safeInitiativeId)
                .filter(response -> "ONBOARDING_OK".equals(response.status()))
                .flatMap(response -> rewardContextHolderService.getInitiativeConfig(safeInitiativeId)
                        .filter(initiativeConfig -> checkInitiativeValidity(initiativeConfig, trxDate))
                        .filter(initiativeConfig -> hasValidEntityReference(initiativeConfig, response.familyId(), safeUserId, safeInitiativeId))
                        .map(initiativeConfig -> Pair.of(initiativeConfig,
                            new BaseOnboardingInfo(safeInitiativeId, response.familyId()))));
    }

    private boolean hasValidEntityReference(InitiativeConfig initiativeConfig, String familyId, String userId, String initiativeId) {
        if (InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(initiativeConfig.getBeneficiaryType())
                && (familyId == null || familyId.isBlank())) {
            log.warn("[REWARD][IS_ONBOARDED] Missing familyId for NF initiative. userId: {}, initiativeId: {}", userId, initiativeId);
            return false;
        }
        return true;
    }

    private boolean checkInitiativeValidity(InitiativeConfig initiativeConfig, OffsetDateTime trxDate) {
        return initiativeConfig != null
                && (initiativeConfig.getStartDate() == null || !initiativeConfig.getStartDate().isAfter(trxDate.toLocalDate()))
                && (initiativeConfig.getEndDate() == null || !initiativeConfig.getEndDate().isBefore(trxDate.toLocalDate()));
    }

}
