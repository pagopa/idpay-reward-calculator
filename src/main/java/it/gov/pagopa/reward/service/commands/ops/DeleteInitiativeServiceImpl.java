package it.gov.pagopa.reward.service.commands.ops;

import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
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
    private final long delay;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
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
        this.delay = delay;
    }

    @Override
    public Mono<String> execute(String initiativeId) {
        log.info("[DELETE_INITIATIVE] Starting handle delete initiative {}", initiativeId);
        Duration delayDuration = Duration.ofMillis(delay);
        Mono<Long> monoDelay = Mono.delay(delayDuration);
        return deleteTransactionProcessed(initiativeId, monoDelay)
                .then(deleteRewardDroolsRule(initiativeId))
                .then(deleteHpanInitiatives(initiativeId, monoDelay))
                .then(deleteEntityCounters(initiativeId, delayDuration))
                .then(removedAfterInitiativeDeletion(delayDuration))
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

    private Mono<Void> deleteHpanInitiatives(String initiativeId, Mono<Long> monoDelay){
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

    private Mono<Void> deleteTransactionProcessed(String initiativeId, Mono<Long> monoDelay){
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

    private Mono<Void> deleteEntityCounters(String initiativeId, Duration delayDuration){
        return userInitiativeCountersRepository.findByInitiativesWithBatch(initiativeId, pageSize)
                .flatMap(counterToDelete -> userInitiativeCountersRepository.deleteById(counterToDelete.getId())
                        .then(Mono.just(counterToDelete)).delayElement(delayDuration), pageSize)
                .doOnNext(deletedCounter -> {
                    log.info("[DELETE_INITIATIVE] Deleted counter with entityId{} on initiative {}", deletedCounter.getEntityId(), initiativeId);
                    auditUtilities.logDeletedEntityCounters(initiativeId, deletedCounter.getEntityId());
                })
                .then();
    }

    public Mono<Void> removedAfterInitiativeDeletion(Duration delayDuration){
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
