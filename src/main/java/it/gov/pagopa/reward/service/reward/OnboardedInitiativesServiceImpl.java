package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

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
            return getInitiatives(trx.getHpan(), trx.getTrxChargeDate(), null);
        } else {
            if(trx.getRefundInfo() != null){
                return Flux.fromIterable(trx.getRefundInfo().getPreviousRewards().keySet());
            } else {
                log.trace("[REWARD] [REWARD_KO] Recognized REFUND operation without previous rewards");
                return Flux.empty();
            }
        }
    }

    @Override
    public Mono<Boolean> isOnboarded(String hpan, OffsetDateTime trxDate, String initiativeId) {
        return getInitiatives(hpan,trxDate,Set.of(initiativeId)).hasElements();
    }

    /** true if > 0 */
    private boolean isPositive(BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) < 0;
    }

    private Flux<String> getInitiatives(String hpan, OffsetDateTime trxDate, Set<String> initiatives) {
        log.trace("[REWARD] Retrieving hpan initiatives onboarded in trxDate: {} - {}", hpan, trxDate);
        return hpanInitiativesRepository.findById(hpan)
                .flatMapMany(initiativesForHpan -> {
                    LocalDateTime trxDateTime = trxDate.atZoneSameInstant(CommonConstants.ZONEID).toLocalDateTime();

                    if (initiativesForHpan != null && initiativesForHpan.getOnboardedInitiatives() != null) {
                        return Flux.fromIterable(initiativesForHpan.getOnboardedInitiatives())
                                .flatMap(i -> rewardContextHolderService.getInitiativeConfig(i.getInitiativeId())
                                        .filter(initiativeConfig -> (initiatives == null || initiatives.contains(initiativeConfig.getInitiativeId()))
                                                && checkInitiativeValidity(initiativeConfig, trxDate)
                                                && checkDate(trxDateTime, i.getActiveTimeIntervals()))
                                        .map(InitiativeConfig::getInitiativeId));
                    }
                    return Flux.empty();
                });
    }

    private boolean checkInitiativeValidity(InitiativeConfig initiativeConfig, OffsetDateTime trxDate) {
        return initiativeConfig != null
                && (initiativeConfig.getStartDate() == null || !initiativeConfig.getStartDate().isAfter(trxDate.toLocalDate()))
                && (initiativeConfig.getEndDate() == null || !initiativeConfig.getEndDate().isBefore(trxDate.toLocalDate()));
    }

    //scroll the ActiveTimeInterval list from the end to facilitate the exit of the for-cycle
    boolean checkDate(LocalDateTime trxDate, List<ActiveTimeInterval> timeIntervals){
        for (int i = timeIntervals.size()-1; i>=0;i--) {
            LocalDateTime start = timeIntervals.get(i).getStartInterval();
            LocalDateTime end = timeIntervals.get(i).getEndInterval();
            if (!trxDate.isBefore(start) && (end == null || trxDate.isBefore(end))) {
                return true;
            }
        }
        return false;
    }
}