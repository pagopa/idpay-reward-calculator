package it.gov.pagopa.reward.service.build;


import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<KieBase> build(Flux<DroolsRule> rules) {
        return Mono.defer(() -> {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = KieServices.get().newKieFileSystem();

            return rules.map(r -> kieFileSystem.write(String.format("src/main/resources/%s/%s.drl", RULES_BUILT_DIR, r.getName()), r.getRule()))
                    .then(Mono.fromSupplier(() -> {
                        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
                        kieBuilder.buildAll();

                        if (kieBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                            throw new IllegalArgumentException("Build Errors:" + kieBuilder.getResults().toString());
                        }

                        KieModule kieModule = kieBuilder.getKieModule();
                        KieBase newKieBase = kieServices.newKieContainer(kieModule.getReleaseId()).getKieBase();

                        log.info("Build completed");
                        if (log.isDebugEnabled()) {
                            KiePackage kiePackage = newKieBase.getKiePackage(RULES_BUILT_PACKAGE);
                            log.debug("The container now will contain the following rules inside %s package: %s".formatted(
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
