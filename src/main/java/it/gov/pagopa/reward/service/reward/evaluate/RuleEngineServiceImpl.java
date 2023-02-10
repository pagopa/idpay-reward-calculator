package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionDroolsMapper;
import it.gov.pagopa.reward.dto.mapper.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.PerformanceLogger;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
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
    public RewardTransactionDTO applyRules(TransactionDTO transaction, List<String> initiatives, UserInitiativeCounters userInitiativeCounters) {
        TransactionDroolsDTO trx = transaction2TransactionDroolsMapper.apply(transaction);

        if(!initiatives.isEmpty()){
            StatelessKieSession statelessKieSession = rewardContextHolderService.getRewardRulesKieBase().newStatelessKieSession();

            trx.setInitiatives(initiatives);
            trx.setRewards(new HashMap<>());

            userInitiativeCounters = setRefundCounters(transaction, userInitiativeCounters);

            List<Command<?>> cmds = new ArrayList<>();
            cmds.add(CommandFactory.newInsert(ruleEngineConfig));
            cmds.add(CommandFactory.newInsert(userInitiativeCounters));
            cmds.add(CommandFactory.newInsert(trx));
            for (String initiative: initiatives) {
                cmds.add(new AgendaGroupSetFocusCommand(initiative));
            }

            long before=System.currentTimeMillis();
            statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));

            PerformanceLogger.logTiming("[REWARD_RULE_ENGINE]", before , "transaction evaluated (%s) and resulted into rewards:(%s), initiativeRejectionReason:%s".formatted(
                    trx.getId(), trx.getRewards(), trx.getInitiativeRejectionReasons()));
        }else {
            trx.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE);
        }
        return transactionDroolsDTO2RewardTransactionMapper.apply(trx);
    }

    private UserInitiativeCounters setRefundCounters(TransactionDTO transaction, UserInitiativeCounters userInitiativeCounters) {
        if(OperationType.REFUND.equals(transaction.getOperationTypeTranscoded()) && transaction.getRefundInfo()!=null && !CollectionUtils.isEmpty(transaction.getRefundInfo().getPreviousTrxs())) {
            userInitiativeCounters = userInitiativeCounters.toBuilder()
                    .initiatives(
                            userInitiativeCounters.getInitiatives().entrySet().stream()
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
        return userInitiativeCounters;
    }
}