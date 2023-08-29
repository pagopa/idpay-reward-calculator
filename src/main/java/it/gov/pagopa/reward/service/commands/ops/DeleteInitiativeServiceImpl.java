package it.gov.pagopa.reward.service.commands.ops;

import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.model.TransactionProcessed;
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
                .then(removedAfterInitiativeDeletion())
                .then(Mono.just(initiativeId));

    }

    private Mono<Void> deleteRewardDroolsRule(String initiativeId){
        return droolsRuleRepository.deleteById(initiativeId)
                .doOnNext(v -> {
                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: reward_rule", initiativeId);
                    auditUtilities.logDeletedRewardDroolRule(initiativeId);
                })
                .then(rewardContextHolderService.refreshKieContainerCacheMiss())
                .then();
    }

    private Mono<Void> deleteHpanInitiatives(String initiativeId){
        return hpanInitiativesRepository.removeInitiativeOnHpan(initiativeId)
                .doOnNext(updateResult -> {
                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: hpan_initiatives_lookup", initiativeId);
                    auditUtilities.logDeletedHpanInitiative(initiativeId, updateResult.getModifiedCount());
                })
                .then();
    }

    private Mono<Void> deleteTransactionProcessed(String initiativeId){
        return rewardContextHolderService.getInitiativeConfig(initiativeId)
                .flatMap(initiativeConfig -> {
                    if(InitiativeRewardType.DISCOUNT.equals(initiativeConfig.getInitiativeRewardType())){
                        return transactionProcessedRepository.removeByInitiativeId(initiativeId)
                                .doOnNext(deleteResult -> {
                                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: transactions_processed", initiativeId);
                                    auditUtilities.logDeletedTransaction(initiativeId, deleteResult.getDeletedCount());
                                })
                                .then();
                    }
                    else {
                        return transactionProcessedRepository.removeInitiativeOnTransaction(initiativeId)
                                .doOnNext(updateResult -> {
                                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: transactions_processed", initiativeId);
                                    auditUtilities.logDeletedTransaction(initiativeId, updateResult.getModifiedCount());
                                })
                                .then();
                    }
                });
    }

    private Mono<Void> deleteEntityCounters(String initiativeId){
        return userInitiativeCountersRepository.deleteByInitiativeId(initiativeId)
                .map(UserInitiativeCounters::getEntityId)
                .doOnNext(entityId -> {
                    log.info("[DELETE_INITIATIVE] Deleted counter with entityId{} on initiative {}", entityId, initiativeId);
                    auditUtilities.logDeletedEntityCounters(initiativeId, entityId);
                })
                .then();
    }

    public Mono<Void> removedAfterInitiativeDeletion(){
        return hpanInitiativesRepository.deleteHpanWithoutInitiative()
                .doOnNext(hpanInitiative -> auditUtilities.logDeletedHpan(hpanInitiative.getHpan(), hpanInitiative.getUserId()))
                .then(transactionProcessedRepository.deleteTransactionsWithoutInitiative()
                        .distinct(TransactionProcessed::getUserId)
                        .doOnNext(trx -> auditUtilities.logDeletedTransactionForUser(trx.getUserId()))
                        .then());
    }
}
