package it.gov.pagopa.reward.service.reward;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DroolsContainerHolderServiceImpl implements  DroolsContainerHolderService{

    private final KieServices kieServices = KieServices.Factory.get();
    private KieContainer kieContainer;

    public DroolsContainerHolderServiceImpl() {
        refreshKieContainer();
    }

    @Override
    public KieContainer getKieContainer() {
        return kieContainer;
    }

    //TODO use cache
    @Scheduled(fixedRateString = "${app.rules.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        log.trace("Refreshing KieContainer");
        //TODO access to DB read rules
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newClassPathResource("rules.drl"));
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
