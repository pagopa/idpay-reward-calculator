package it.gov.pagopa.reward.service.build;


import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.KieContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Service
@Slf4j
public class KieContainerBuilderServiceImpl implements KieContainerBuilderService {

    public static final String rulesBuiltPackage = "it.gov.pagopa.reward.drools.buildrules";
    private static final String rulesBuiltDir = rulesBuiltPackage.replace(".", "/");

    private final DroolsRuleRepository droolsRuleRepository;

    public KieContainerBuilderServiceImpl(DroolsRuleRepository droolsRuleRepository) {
        this.droolsRuleRepository = droolsRuleRepository;
    }

    @Override
    public Mono<KieContainer> buildAll() {
        log.info("Fetching and building all the initiatives");
        return build(droolsRuleRepository.findAll());
    }

    @Override
    public Mono<KieContainer> build(Flux<DroolsRule> rules) {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = KieServices.get().newKieFileSystem();

        return rules.map(r -> kieFileSystem.write(String.format("src/main/resources/%s/%s.drl", rulesBuiltDir, r.getName()), r.getRule()))
                .then(Mono.fromSupplier(() -> {
                    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
                    kieBuilder.buildAll();
                    /* TODO check and notify errors
                    if (kb.getResults().hasMessages(Message.Level.ERROR)) {
                        throw new IllegalArgumentException("Build Errors:" + kb.getResults().toString());
                    }*/
                    KieModule kieModule = kieBuilder.getKieModule();
                    KieContainer newKieContainer = kieServices.newKieContainer(kieModule.getReleaseId());

                    log.info("Build completed");
                    if (log.isDebugEnabled()) {
                        KiePackage kiePackage = newKieContainer.getKieBase().getKiePackage(rulesBuiltPackage);
                        log.debug("The container now will contain the following rules inside %s package: %s".formatted(
                                rulesBuiltPackage,
                                kiePackage != null
                                        ? kiePackage.getRules().stream().map(Rule::getId).collect(Collectors.toList())
                                        : "0"));
                    }
                    return newKieContainer;
                }));
    }
}
