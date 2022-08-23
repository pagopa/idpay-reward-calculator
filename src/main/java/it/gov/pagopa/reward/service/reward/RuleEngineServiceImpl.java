package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionDroolsMapper;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
            StatelessKieSession statelessKieSession = rewardContextHolderService.getRewardRulesKieContainer().newStatelessKieSession();

            trx.setInitiatives(initiatives);
            trx.setRewards(new HashMap<>());

            List<Command<?>> cmds = new ArrayList<>();
            cmds.add(CommandFactory.newInsert(ruleEngineConfig));
            cmds.add(CommandFactory.newInsert(userInitiativeCounters));
            cmds.add(CommandFactory.newInsert(trx));
            for (String initiative: initiatives) {
                cmds.add(new AgendaGroupSetFocusCommand(initiative));
            }

            long before=System.currentTimeMillis();
            statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));
            long after=System.currentTimeMillis();

            log.info("Time between before and after fireAllRules: {} ms", after-before);

            /* TODO uncomment
            if(log.isDebugEnabled()){
                log.debug("Time between before and after fireAllRules: {} ms", after-before);
            }
            */

            log.debug("Send message prepared: {}", trx);
            log.info("Transaction evaluated  ({}) and resulted into rewards:({}), initiativeRejectionReason:{}", "%s-%s".formatted(trx.getHpan(), trx.getTrxDate()), trx.getRewards(), trx.getInitiativeRejectionReasons());
        }else {
            trx.getRejectionReasons().add(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE);
        }
        return transactionDroolsDTO2RewardTransactionMapper.apply(trx);
    }
}
