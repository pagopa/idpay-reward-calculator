package it.gov.pagopa.service.reward;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.mapper.RewardTransactionMapper;
import it.gov.pagopa.dto.mapper.TransactionMapper;
import it.gov.pagopa.model.RewardTransaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
    private final TransactionMapper transactionMapper;
    private final RewardTransactionMapper rewardTransactionMapper;

    public RuleEngineServiceImpl(DroolsContainerHolderService droolsContainerHolderService, TransactionMapper transactionMapper, RewardTransactionMapper rewardTransactionMapper) {
        this.droolsContainerHolderService = droolsContainerHolderService;
        this.transactionMapper = transactionMapper;
        this.rewardTransactionMapper = rewardTransactionMapper;
    }

    @Override
    public RewardTransactionDTO applyRules(TransactionDTO transaction, List<String> initiatives) {

        if(CollectionUtils.isNotEmpty(initiatives)){
            Instant before;
            Instant after;

            StatelessKieSession statelessKieSession = droolsContainerHolderService.getKieContainer().newStatelessKieSession();

            RewardTransaction trx = transactionMapper.map(transaction);
            trx.setInitiatives(initiatives);
            trx.setRewards(new HashMap<>());

            List<Command> cmds = new ArrayList<>();
            cmds.add(CommandFactory.newInsert(trx));
            for (String initiative: initiatives) {
                cmds.add(new AgendaGroupSetFocusCommand(initiative));
            }

            before=Instant.now();
            statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));
            after=Instant.now();
            log.info("Time between before and after fireAllRules: {} ms", Duration.between(before, after).toMillis());

            log.info("Send message prepared: {}", trx);
            return rewardTransactionMapper.map(trx);
        }else {
            return null;
        }
    }
}
