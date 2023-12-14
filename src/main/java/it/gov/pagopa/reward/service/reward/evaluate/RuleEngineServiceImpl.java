package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2TransactionDroolsMapper;
import it.gov.pagopa.reward.dto.mapper.trx.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RuleEngineServiceImpl implements RuleEngineService {
    private final RuleEngineConfig ruleEngineConfig;
    private final RewardContextHolderService rewardContextHolderService;
    private final Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper;
    private final TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper;

    public RuleEngineServiceImpl(RuleEngineConfig ruleEngineConfig,
                                 RewardContextHolderService rewardContextHolderService,
                                 Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper,
                                 TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper) {
        this.ruleEngineConfig = ruleEngineConfig;
        this.rewardContextHolderService = rewardContextHolderService;
        this.transaction2TransactionDroolsMapper = transaction2TransactionDroolsMapper;
        this.transactionDroolsDTO2RewardTransactionMapper = transactionDroolsDTO2RewardTransactionMapper;
    }

    @Override
    public RewardTransactionDTO applyRules(TransactionDTO transaction, List<String> initiatives, UserInitiativeCountersWrapper userInitiativeCountersWrapper) {
        TransactionDroolsDTO trx = transaction2TransactionDroolsMapper.apply(transaction);

        if(!initiatives.isEmpty()){
            StatelessKieSession statelessKieSession = rewardContextHolderService.getRewardRulesKieBase().newStatelessKieSession();

            rejectInitiativeNotInKieBase(initiatives, trx);

            trx.setInitiatives(initiatives);
            trx.setRewards(new HashMap<>());

            userInitiativeCountersWrapper = setRefundCounters(transaction, userInitiativeCountersWrapper);

            List<Command<?>> cmds = new ArrayList<>();
            cmds.add(CommandFactory.newInsert(ruleEngineConfig));
            cmds.add(CommandFactory.newInsert(userInitiativeCountersWrapper));
            cmds.add(CommandFactory.newInsert(trx));
            for (String initiative: initiatives) {
                cmds.add(new AgendaGroupSetFocusCommand(initiative));
            }

            long before=System.currentTimeMillis();
            statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));

            PerformanceLogger.logTiming("REWARD_RULE_ENGINE", before , "transaction evaluated (%s) and resulted into rewards:(%s), initiativeRejectionReason:%s".formatted(
                    trx.getId(), trx.getRewards(), trx.getInitiativeRejectionReasons()));
        }else {
            trx.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE);
        }
        return transactionDroolsDTO2RewardTransactionMapper.apply(trx);
    }

    private static final List<String> INITIATIVE_REJECTION_REASON_RULE_ENGINE_NOT_READY = List.of(RewardConstants.TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY);

    private void rejectInitiativeNotInKieBase(List<String> initiatives, TransactionDroolsDTO trx) {
        Set<String> rewardRulesKieInitiativeIds = rewardContextHolderService.getRewardRulesKieInitiativeIds();

        trx.getInitiativeRejectionReasons().putAll(
                initiatives.stream()
                        .filter(i -> !rewardRulesKieInitiativeIds.contains(i)) // the initiative is not inside the container drools
                        .collect(Collectors.toMap(Function.identity(), i -> INITIATIVE_REJECTION_REASON_RULE_ENGINE_NOT_READY))
        );
    }

    private UserInitiativeCountersWrapper setRefundCounters(TransactionDTO transaction, UserInitiativeCountersWrapper userInitiativeCountersWrapper) {
        if(OperationType.REFUND.equals(transaction.getOperationTypeTranscoded()) && transaction.getRefundInfo()!=null && !CollectionUtils.isEmpty(transaction.getRefundInfo().getPreviousTrxs())) {
            userInitiativeCountersWrapper = userInitiativeCountersWrapper.toBuilder()
                    .initiatives(
                            userInitiativeCountersWrapper.getInitiatives().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> e.getValue().toBuilder()
                                                    .trxNumber(
                                                            Optional.ofNullable(transaction.getRefundInfo().getPreviousTrxs().get(0).getRewards().get(e.getKey()))
                                                                    .map(r->r.getCounters().getTrxNumber()-1)
                                                                    .orElse(e.getValue().getTrxNumber())
                                                    )
                                                    .build()
                                    )))
                    .build();
        }
        return userInitiativeCountersWrapper;
    }
}