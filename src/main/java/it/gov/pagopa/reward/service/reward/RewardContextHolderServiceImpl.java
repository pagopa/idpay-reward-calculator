package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class RewardContextHolderServiceImpl implements RewardContextHolderService {

    private final KieContainerBuilderService kieContainerBuilderService;
    private final Map<String, InitiativeConfig> initiativeId2Config=new HashMap<>();
    private final DroolsRuleRepository droolsRuleRepository;

    private KieContainer kieContainer;

    public RewardContextHolderServiceImpl(KieContainerBuilderService kieContainerBuilderService, DroolsRuleRepository droolsRuleRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.kieContainerBuilderService =kieContainerBuilderService;
        this.droolsRuleRepository = droolsRuleRepository;
        refreshKieContainer(x -> applicationEventPublisher.publishEvent(new RewardContextHolderReadyEvent(this)));
    }

    public static class RewardContextHolderReadyEvent extends ApplicationEvent {
        public RewardContextHolderReadyEvent(Object source) {
            super(source);
        }
    }

    //region kieContainer holder
    @Override
    public KieContainer getRewardRulesKieContainer() {
        return kieContainer;
    }

    @Override
    public void setRewardRulesKieContainer(KieContainer kieContainer) {
        this.kieContainer=kieContainer;
        //TODO store in cache
    }

    @Scheduled(fixedRateString = "${app.reward-rule.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        refreshKieContainer(x -> log.trace("Refreshed KieContainer"));
    }

    //TODO use cache
    public void refreshKieContainer(Consumer<? super KieContainer> subscriber){
        log.trace("Refreshing KieContainer");
        final Flux<DroolsRule> droolsRuleFlux = droolsRuleRepository.findAll().doOnNext(dr -> setInitiativeConfig(dr.getInitiativeConfig()));

        kieContainerBuilderService.build(droolsRuleFlux).doOnNext(this::setRewardRulesKieContainer).subscribe(subscriber);
    }

    //endregion

    //region initiativeConfig holder
    @Override
    public InitiativeConfig getInitiativeConfig(String initiativeId) {
        return initiativeId2Config.computeIfAbsent(initiativeId, this::retrieveInitiativeConfig);
    }

    @Override
    public void setInitiativeConfig(InitiativeConfig initiativeConfig) {
        initiativeId2Config.put(initiativeConfig.getInitiativeId(),initiativeConfig);
    }

    private InitiativeConfig retrieveInitiativeConfig(String initiativeId) {
        DroolsRule droolsRule = droolsRuleRepository.findById(initiativeId).block();
        if (droolsRule==null){
            log.error("cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        return droolsRule.getInitiativeConfig();
    }
    //endregion

}
