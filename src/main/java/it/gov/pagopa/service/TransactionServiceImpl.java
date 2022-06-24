package it.gov.pagopa.service;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.mapper.RewardsTransactionDTO;
import it.gov.pagopa.dto.mapper.TransactionMapper;
import it.gov.pagopa.model.RewardTransaction;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    private final StatelessKieSession statelessKieSession;
    private final TransactionMapper trxMapper;

    public TransactionServiceImpl(StatelessKieSession statelessKieSession, TransactionMapper trxMapper) {
        this.statelessKieSession = statelessKieSession;
        this.trxMapper = trxMapper;
    }

    @Override
    public RewardsTransactionDTO applyRules(TransactionDTO transaction) {
        Instant before;
        Instant after;

        RewardTransaction trx = trxMapper.map(transaction);

        List<String> initiative = findInitiatives();
        Map<String, BigDecimal> rewards = new HashMap<>();

        for (String initiativeChoose : initiative){

            RewardTransaction trxTemporal = trx;
            List<Command> cmds = new ArrayList<>();
            cmds.add(CommandFactory.newInsert(trx));
            cmds.add(new AgendaGroupSetFocusCommand(initiativeChoose));

            before=Instant.now();
            statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));
            after=Instant.now();
            log.info("Time between before and after fireAllRules: {} ms", Duration.between(before, after).toMillis());
            if(trxTemporal.getReward()!=null){
                rewards.put(initiativeChoose,trxTemporal.getReward());
            }
        }
        return new RewardsTransactionDTO(transaction,rewards);
    }

    public List<String> findInitiatives(){
        List<String> initiative = Arrays.asList("ini001","ini002","ini003","ini004");
        return initiative.subList(1,3);
    }
}
