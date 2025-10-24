package it.gov.pagopa.reward.service.commands.ops;

import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.reward.connector.repository.secondary.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.secondary.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.primary.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class DeleteInitiativeServiceImpl implements DeleteInitiativeService{
    private final DroolsRuleRepository droolsRuleRepository;
    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final TransactionProcessedRepository transactionProcessedRepository;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final RewardContextHolderService rewardContextHolderService;
    private final AuditUtilities auditUtilities;
    private final int pageSize;
    private final Duration delayDuration;
    private final Mono<Long> monoDelay;


    public DeleteInitiativeServiceImpl(DroolsRuleRepository droolsRuleRepository,
                                       HpanInitiativesRepository hpanInitiativesRepository,
                                       TransactionProcessedRepository transactionProcessedRepository,
                                       UserInitiativeCountersRepository userInitiativeCountersRepository,
                                       RewardContextHolderService rewardContextHolderService, AuditUtilities auditUtilities,
                                       @Value("${app.delete.paginationSize}")int pageSize,
                                       @Value("${app.delete.delayTime}")long delay) {

        this.droolsRuleRepository = droolsRuleRepository;
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.transactionProcessedRepository = transactionProcessedRepository;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.rewardContextHolderService = rewardContextHolderService;
        this.auditUtilities = auditUtilities;
        this.pageSize = pageSize;
        this.delayDuration = Duration.ofMillis(delay);
        this.monoDelay = Mono.delay(delayDuration);
    }

    @Override
    public Mono<String> execute(String initiativeId) {
        log.info("[DELETE_INITIATIVE] Starting handle delete initiative {}", initiativeId);
        return execAndLogTiming("DELETE_TRANSACTION_PROCESSED", initiativeId, deleteTransactionProcessed(initiativeId))
                .then(execAndLogTiming("DELETE_DROOLS_RULE", initiativeId, deleteRewardDroolsRule(initiativeId)))
                .then(execAndLogTiming("DELETE_HPAN_LOOKUP", initiativeId, deleteHpanInitiatives(initiativeId)))
                .then(execAndLogTiming("DELETE_COUNTERS", initiativeId, deleteEntityCounters(initiativeId)))
                .then(execAndLogTiming("CLEAN_AFTER_DELETE", initiativeId, removedAfterInitiativeDeletion()))
                .then(Mono.just(initiativeId));

    }

    private Mono<?> execAndLogTiming(String deleteFlowName, String initiativeId, Mono<?> deleteMono) {
        return PerformanceLogger.logTimingFinally(deleteFlowName, deleteMono, initiativeId);
    }

    private Mono<Void> deleteRewardDroolsRule(String initiativeId){
        return droolsRuleRepository.removeById(initiativeId)
                .doOnNext(v -> {
                    log.info("[DELETE_INITIATIVE] Deleted {} initiative {} from collection: reward_rule", v.getDeletedCount(), initiativeId);
                    auditUtilities.logDeletedRewardDroolRule(initiativeId);
                })
                .then();
    }

    private Mono<Void> deleteHpanInitiatives(String initiativeId){
        return hpanInitiativesRepository.findByInitiativesWithBatch(initiativeId, pageSize)
                .flatMap(hpanToUpdate -> hpanInitiativesRepository.removeInitiativeOnHpan(hpanToUpdate.getHpan(), initiativeId)
                        .then(monoDelay), pageSize)
                .count()
                .doOnNext(totalHpansUpdated -> {
                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: hpan_initiatives_lookup", initiativeId);
                    auditUtilities.logDeletedHpanInitiative(initiativeId, totalHpansUpdated);
                })
                .then();
    }

    private Mono<Void> deleteTransactionProcessed(String initiativeId){
        return rewardContextHolderService.getInitiativeConfig(initiativeId)
                .flatMap(initiativeConfig -> {
                    if(InitiativeRewardType.DISCOUNT.equals(initiativeConfig.getInitiativeRewardType())){
                        return transactionProcessedRepository.findByInitiativesWithBatch(initiativeId, pageSize)
                                .flatMap(trxToDelete -> transactionProcessedRepository.deleteById(trxToDelete.getId())
                                        .then(monoDelay), pageSize)
                                .count()
                                .doOnNext(totalTrxDeleted -> {
                                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: transactions_processed", initiativeId);
                                    auditUtilities.logDeletedTransaction(initiativeId, totalTrxDeleted);
                                })
                                .then();
                    }
                    else {
                        return transactionProcessedRepository.findByInitiativesWithBatch(initiativeId, pageSize)
                                .flatMap(trxToUpdate -> transactionProcessedRepository.removeInitiativeOnTransaction(trxToUpdate.getId(), initiativeId)
                                        .then(monoDelay), pageSize)
                                .count()
                                .doOnNext(totalTrxUpdated -> {
                                    log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: transactions_processed", initiativeId);
                                    auditUtilities.logDeletedTransaction(initiativeId, totalTrxUpdated);
                                })
                                .then();
                    }
                });
    }

    private Mono<Void> deleteEntityCounters(String initiativeId){
        return userInitiativeCountersRepository.findByInitiativesWithBatch(initiativeId, pageSize)
                .flatMap(counterToDelete -> userInitiativeCountersRepository.deleteById(counterToDelete.getId())
                        .then(Mono.just(counterToDelete)).delayElement(delayDuration), pageSize)
                .doOnNext(deletedCounter -> {
                    log.info("[DELETE_INITIATIVE] Deleted counter with entityId{} on initiative {}", deletedCounter.getEntityId(), initiativeId);
                    auditUtilities.logDeletedEntityCounters(initiativeId, deletedCounter.getEntityId());
                })
                .then();
    }

    public Mono<Void> removedAfterInitiativeDeletion(){
        return hpanInitiativesRepository.findWithoutInitiativesWithBatch(pageSize)
                .flatMap(hpanToDelete -> hpanInitiativesRepository.deleteById(hpanToDelete.getHpan())
                        .then(Mono.just(hpanToDelete)).delayElement(delayDuration), pageSize)
                .doOnNext(hpanInitiative -> auditUtilities.logDeletedHpan(hpanInitiative.getHpan(), hpanInitiative.getUserId()))
                .then(transactionProcessedRepository.findWithoutInitiativesWithBatch(pageSize)
                        .flatMap(trxToDelete -> transactionProcessedRepository.deleteById(trxToDelete.getId())
                                .then(Mono.just(trxToDelete)).delayElement(delayDuration), pageSize)
                        .distinct(TransactionProcessed::getUserId)
                        .doOnNext(trx -> auditUtilities.logDeletedTransactionForUser(trx.getUserId()))
                        .then());
    }
}
