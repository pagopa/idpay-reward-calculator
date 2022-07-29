package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.TransactionDroolsDTO2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionDroolsMapper;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class RuleEngineServiceImpl implements RuleEngineService {
    private final DroolsContainerHolderService droolsContainerHolderService;
    private final Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper;
    private final TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper;

    public RuleEngineServiceImpl(DroolsContainerHolderService droolsContainerHolderService, Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper, TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper) {
        this.droolsContainerHolderService = droolsContainerHolderService;
        this.transaction2TransactionDroolsMapper = transaction2TransactionDroolsMapper;
        this.transactionDroolsDTO2RewardTransactionMapper = transactionDroolsDTO2RewardTransactionMapper;
    }

    @Override
    public RewardTransactionDTO applyRules(TransactionDTO transaction, List<String> initiatives, UserInitiativeCounters userInitiativeCounters) {
        TransactionDroolsDTO trx = transaction2TransactionDroolsMapper.apply(transaction);

        if(!initiatives.isEmpty()){
            Instant before;
            Instant after;

            StatelessKieSession statelessKieSession = droolsContainerHolderService.getRewardRulesKieContainer().newStatelessKieSession();

            trx.setInitiatives(initiatives);
            trx.setRewards(new HashMap<>());

            List<Command<?>> cmds = new ArrayList<>();
            cmds.add(CommandFactory.newInsert(trx));
            for (String initiative: initiatives) {
                cmds.add(new AgendaGroupSetFocusCommand(initiative));
            }
            log.info(cmds.toString());

            before=Instant.now();
            statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));
            after=Instant.now();
            log.info("Time between before and after fireAllRules: {} ms", Duration.between(before, after).toMillis());

            log.info("Send message prepared: {}", trx);
        }else {
            // The date of transaction is not in an active range for the hpan
            trx.setRejectionReasons(List.of("HPAN_NOT_ACTIVE"));
        }
        return transactionDroolsDTO2RewardTransactionMapper.apply(trx);
    }
}
