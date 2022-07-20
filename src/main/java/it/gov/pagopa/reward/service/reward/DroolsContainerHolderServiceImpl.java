package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DroolsContainerHolderServiceImpl implements  DroolsContainerHolderService{

    private final KieContainerBuilderService kieContainerBuilderService;
    private KieContainer kieContainer;

    public DroolsContainerHolderServiceImpl(KieContainerBuilderService kieContainerBuilderService) {
        this.kieContainerBuilderService =kieContainerBuilderService;
        refreshKieContainer();
    }

    @Override
    public KieContainer getKieContainer() {
        return kieContainer;
    }

    @Override
    public void setKieContainer(KieContainer kieContainer) {
        this.kieContainer=kieContainer;
    }

    //TODO use cache
    @Scheduled(fixedRateString = "${app.rules.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        log.trace("Refreshing KieContainer");
        kieContainerBuilderService.buildAll().subscribe(this::setKieContainer);
    }
}
