package it.gov.pagopa.service;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.mapper.TransactionMapper;
import it.gov.pagopa.model.RewardTransaction;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    public RewardTransaction applyRules(TransactionDTO transaction) {
        Instant before;
        Instant after;
        RewardTransaction trx = trxMapper.map(transaction);

        List<Command> cmds = new ArrayList<>();
        cmds.add(CommandFactory.newInsert(trx));
        cmds.add(CommandFactory.newFireAllRules());

        before=Instant.now();
        statelessKieSession.execute(CommandFactory.newBatchExecution(cmds));
        after=Instant.now();
        log.info("Time between before and after fireAllRules: {} ms", Duration.between(before, after).toMillis());
        return trx;
    }
}
