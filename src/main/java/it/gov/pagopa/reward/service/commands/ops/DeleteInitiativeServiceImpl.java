package it.gov.pagopa.reward.service.commands.ops;

import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DeleteInitiativeServiceImpl implements DeleteInitiativeService{
    private final DroolsRuleRepository droolsRuleRepository;
    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final TransactionProcessedRepository transactionProcessedRepository;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final RewardContextHolderService rewardContextHolderService;
    private final AuditUtilities auditUtilities;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public DeleteInitiativeServiceImpl(DroolsRuleRepository droolsRuleRepository,
                                       HpanInitiativesRepository hpanInitiativesRepository,
                                       TransactionProcessedRepository transactionProcessedRepository,
                                       UserInitiativeCountersRepository userInitiativeCountersRepository,
                                       RewardContextHolderService rewardContextHolderService, AuditUtilities auditUtilities) {

        this.droolsRuleRepository = droolsRuleRepository;
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.transactionProcessedRepository = transactionProcessedRepository;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.rewardContextHolderService = rewardContextHolderService;
        this.auditUtilities = auditUtilities;
    }

    @Override
    public Mono<String> execute(String initiativeId) {
        log.info("[DELETE_INITIATIVE] Starting handle delete initiative {}", initiativeId);
        return deleteTransactionProcessed(initiativeId)
                .then(deleteRewardDroolsRule(initiativeId))
                .then(deleteHpanInitiatives(initiativeId))
                .then(deleteEntityCounters(initiativeId))
                .then(Mono.just(initiativeId));

    }

    private Mono<Void> deleteRewardDroolsRule(String initiativeId){
        return droolsRuleRepository.deleteById(initiativeId)
                .doOnNext(v -> auditUtilities.logDeletedRewardDroolRule(
                        initiativeId
                ));
    }

    private Mono<Void> deleteHpanInitiatives(String initiativeId){
        return hpanInitiativesRepository.findAndRemoveInitiativeOnHpan(initiativeId)
                .doOnNext(updateResult ->
                        auditUtilities.logDeletedHpanInitiative(
                                initiativeId,
                                updateResult.getModifiedCount()
                        ))
                .then();
    }

    private Mono<Void> deleteTransactionProcessed(String initiativeId){
        return rewardContextHolderService.getInitiativeConfig(initiativeId)
                .flatMap(initiativeConfig -> {
                    if(InitiativeRewardType.DISCOUNT.equals(initiativeConfig.getInitiativeRewardType())){
                        return transactionProcessedRepository.deleteByInitiativeId(initiativeId)
                                .count()
                                .doOnNext(count -> auditUtilities.logDeletedTransaction(initiativeId, count))
                                .then();
                    }
                    else {
                        return transactionProcessedRepository.findAndRemoveInitiativeOnTransaction(initiativeId)
                                .doOnNext(updateResult -> auditUtilities.logDeletedTransaction(initiativeId, updateResult.getModifiedCount()))
                                .then();
                    }
                });
    }

    private Mono<Void> deleteEntityCounters(String initiativeId){
        return userInitiativeCountersRepository.deleteByInitiativeId(initiativeId)
                .map(UserInitiativeCounters::getUserId)
                .doOnNext(entityId ->
                        auditUtilities.logDeletedEntityCounters(initiativeId, entityId))
                .then();
    }
}
