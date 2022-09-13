package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OnboardedInitiativesServiceImpl implements OnboardedInitiativesService {

    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final RewardContextHolderService rewardContextHolderService;

    public OnboardedInitiativesServiceImpl(HpanInitiativesRepository hpanInitiativesRepository, RewardContextHolderService rewardContextHolderService){
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.rewardContextHolderService = rewardContextHolderService;
    }

    @Override
    public Flux<String> getInitiatives(TransactionDTO trx) {
        if(OperationType.CHARGE.equals(trx.getOperationTypeTranscoded()) || isPositive(trx.getEffectiveAmount())){
            return getInitiatives(trx.getHpan(), trx.getTrxChargeDate());
        } else {
            if(trx.getReversalInfo() != null){
                return Flux.fromIterable(trx.getReversalInfo()
                        .getPreviousRewards().entrySet().stream()
                        .filter(r-> isPositive(r.getValue().getAccruedReward()))
                        .map(Map.Entry::getKey).toList());
            } else {
                log.trace("[REWARD] [REWARD_KO] Recognized REVERSAL operation without previous rewards");
                return Flux.empty();
            }
        }
    }

    /** true if > 0 */
    private boolean isPositive(BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) < 0;
    }

    private Flux<String> getInitiatives(String hpan, OffsetDateTime trxDate) {
        log.trace("[REWARD] Retrieving hpan initiatives onboarded in trxDate");
        return hpanInitiativesRepository.findById(hpan)
                .flatMapMany(initiativesForHpan -> {
                    LocalDateTime trxDateTime = trxDate.atZoneSameInstant(ZoneId.of("Europe/Rome")).toLocalDateTime();
                    List<String> initiatives = new ArrayList<>();

                    if (initiativesForHpan != null && initiativesForHpan.getOnboardedInitiatives() != null) {
                        List<OnboardedInitiative> onboardedInitiatives = initiativesForHpan.getOnboardedInitiatives();
                        for (OnboardedInitiative i : onboardedInitiatives) {
                            if (checkInitiativeValidity(i.getInitiativeId(), trxDate) && checkDate(trxDateTime, i.getActiveTimeIntervals())) {
                                initiatives.add(i.getInitiativeId());
                            }
                        }
                    }
                    return Flux.fromIterable(initiatives);
                });
    }

    private boolean checkInitiativeValidity(String initiativeId, OffsetDateTime trxDate) {
        InitiativeConfig initiativeConfig = rewardContextHolderService.getInitiativeConfig(initiativeId);
        return initiativeConfig != null && (initiativeConfig.getEndDate() == null || initiativeConfig.getEndDate().isAfter(trxDate.toLocalDate()));
    }

    //scroll the ActiveTimeInterval list from the end to facilitate the exit of the for-cycle
    boolean checkDate(LocalDateTime trxDate, List<ActiveTimeInterval> timeIntervals){
        for (int i = timeIntervals.size()-1; i>=0;i--) {
            LocalDateTime start = timeIntervals.get(i).getStartInterval();
            LocalDateTime end = timeIntervals.get(i).getEndInterval();
            if (!trxDate.isBefore(start) && (end == null || !trxDate.isAfter(end))) {
                return true;
            }
        }
        return false;
    }
}
