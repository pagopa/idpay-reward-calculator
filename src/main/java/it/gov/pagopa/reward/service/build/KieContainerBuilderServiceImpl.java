package it.gov.pagopa.reward.service.build;


import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.command.Command;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class KieContainerBuilderServiceImpl implements KieContainerBuilderService {

    public static final String RULES_BUILT_PACKAGE = "it.gov.pagopa.reward.drools.buildrules";
    private static final String RULES_BUILT_DIR = RULES_BUILT_PACKAGE.replace(".", "/");

    private final DroolsRuleRepository droolsRuleRepository;

    public KieContainerBuilderServiceImpl(DroolsRuleRepository droolsRuleRepository) {
        this.droolsRuleRepository = droolsRuleRepository;
    }

    @Override
    public Mono<KieBase> buildAll() {
        log.info("Fetching and building all the initiatives");
        return build(droolsRuleRepository.findAll());
    }

    @Override
    public void preLoadKieBase(KieBase kieBase) {
        try {
            log.info("[DROOLS_CONTAINER_COMPILE] Starting KieContainer compile");
            long startTime = System.currentTimeMillis();
            TransactionDroolsDTO trx = new TransactionDroolsDTO();
            trx.setEffectiveAmountCents(1_00L);
            trx.setTrxChargeDate(OffsetDateTime.now());
            UserInitiativeCountersWrapper userCounters = new UserInitiativeCountersWrapper();
            userCounters.setInitiatives(new HashMap<>());

            List<Command<?>> cmds = new ArrayList<>();
            cmds.add(CommandFactory.newInsert(new RuleEngineConfig()));
            cmds.add(CommandFactory.newInsert(userCounters));
            cmds.add(CommandFactory.newInsert(trx));
            Arrays.stream(((KnowledgeBaseImpl) kieBase).getPackages()).flatMap(p -> p.getRules().stream()).map(r -> ((RuleImpl) r).getAgendaGroup())
                    .distinct().forEach(a -> cmds.add(new AgendaGroupSetFocusCommand(a)));
            StatelessKieSession session = kieBase.newStatelessKieSession();
            session.execute(CommandFactory.newBatchExecution(cmds));
            long endTime = System.currentTimeMillis();

            log.info("[DROOLS_CONTAINER_COMPILE] KieContainer instance compiled in {} ms", endTime - startTime);
        } catch (Exception e){
            log.warn("[DROOLS_CONTAINER_COMPILE] An error occurred while pre-compiling Drools rules. This will not influence the right behavior of the application, the rules will be compiled the first time they are used", e);
        }
    }

    @Override
    public Mono<KieBase> build(Flux<DroolsRule> rules) {
        return Mono.defer(() -> {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = KieServices.get().newKieFileSystem();

            return rules.map(r -> kieFileSystem.write(String.format("src/main/resources/%s/%s.drl", RULES_BUILT_DIR, r.getId()), r.getRule()))
                    .then(Mono.fromSupplier(() -> {
                        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
                        kieBuilder.buildAll();

                        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                            throw new IllegalArgumentException("Build Errors:" + kieBuilder.getResults().toString());
                        }

                        KieModule kieModule = kieBuilder.getKieModule();
                        KieBase newKieBase = kieServices.newKieContainer(kieModule.getReleaseId()).getKieBase();

                        log.info("[REWARD_RULE_BUILD] Build completed");
                        if (log.isDebugEnabled()) {
                            KiePackage kiePackage = newKieBase.getKiePackage(RULES_BUILT_PACKAGE);
                            log.debug("[REWARD_RULE_BUILD] The container now will contain the following rules inside %s package: %s".formatted(
                                    RULES_BUILT_PACKAGE,
                                    kiePackage != null
                                            ? kiePackage.getRules().stream().map(Rule::getId).toList()
                                            : "0"));
                        }
                        return newKieBase;
                    }));
        });
    }
}
