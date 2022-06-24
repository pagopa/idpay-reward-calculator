package it.gov.pagopa.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringBoot Configuration esclusiva a Drools
 * Vengono caricati/buildati all'avvio il set di regole da applicare a runtime
 */
@Configuration
public class DroolsConfiguration {
    private final KieServices kieServices = KieServices.Factory.get();

    /**
     * Ogni regola in Drools appartiene a un set di regole e l'applicazione richiede un KieSession per eseguire queste regole su un oggetto.
     *
     * @return a {@link StatelessKieSession} instance
     */
    @Bean
    public StatelessKieSession getStatelessKieSession() {
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules.drl"));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId()).newStatelessKieSession();
    }

}
