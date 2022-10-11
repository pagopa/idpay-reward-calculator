package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@Slf4j
public class RewardContextHolderServiceImpl implements RewardContextHolderService {

    private final KieContainerBuilderService kieContainerBuilderService;
    private final Map<String, InitiativeConfig> initiativeId2Config=new ConcurrentHashMap<>();
    private final DroolsRuleRepository droolsRuleRepository;
    private final ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate;
    private KieBase kieBase;
    private final boolean isRedisCacheEnabled;

    public static final String CACHE_ID_REWARD_CONTEXT_HOLDER = "reward_rule";

    public RewardContextHolderServiceImpl(KieContainerBuilderService kieContainerBuilderService, DroolsRuleRepository droolsRuleRepository, ApplicationEventPublisher applicationEventPublisher, @Autowired(required = false) ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate, @Value("${spring.redis.enabled}") boolean isRedisCacheEnabled) {
        this.kieContainerBuilderService =kieContainerBuilderService;
        this.droolsRuleRepository = droolsRuleRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.isRedisCacheEnabled = isRedisCacheEnabled;
        refreshKieContainer(x -> applicationEventPublisher.publishEvent(new RewardContextHolderReadyEvent(this)));
    }

    public static class RewardContextHolderReadyEvent extends ApplicationEvent {
        public RewardContextHolderReadyEvent(Object source) {
            super(source);
        }
    }

    //region kieContainer holder
    @Override
    public KieBase getRewardRulesKieBase() {
        return kieBase;
    }

    @Override
    public void setRewardRulesKieBase(KieBase kieBase) {
        this.kieBase = kieBase;
        if (isRedisCacheEnabled) {
            reactiveRedisTemplate.opsForValue().set(CACHE_ID_REWARD_CONTEXT_HOLDER, SerializationUtils.serialize(kieBase)).subscribe(x -> log.debug("Saving KieContainer in cache"));
        }
    }

    @Scheduled(initialDelayString = "${app.reward-rule.cache.refresh-ms-rate}", fixedRateString = "${app.reward-rule.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        refreshKieContainer(x -> log.trace("Refreshed KieContainer"));
    }

    public void refreshKieContainer(Consumer<? super KieBase> subscriber) {
        if (isRedisCacheEnabled) {
            reactiveRedisTemplate.opsForValue().get(CACHE_ID_REWARD_CONTEXT_HOLDER)
                    .map(c -> (KieBase) SerializationUtils.deserialize(c))
                    .doOnNext(c -> this.kieBase = c)
                    .switchIfEmpty(refreshKieContainerCacheMiss())
                    .subscribe(subscriber);
        } else {
            refreshKieContainerCacheMiss().subscribe(subscriber);
        }
    }

    private Mono<KieBase> refreshKieContainerCacheMiss() {
        log.trace("Refreshing KieContainer");
        final Flux<DroolsRule> droolsRuleFlux = droolsRuleRepository.findAll().doOnNext(dr -> setInitiativeConfig(dr.getInitiativeConfig()));
        return kieContainerBuilderService.build(droolsRuleFlux).doOnNext(this::setRewardRulesKieBase);
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
        log.debug("[CACHE_MISS] Cannot find locally initiativeId {}", initiativeId);
        long startTime = System.currentTimeMillis();
        DroolsRule droolsRule = droolsRuleRepository.findById(initiativeId).block(Duration.ofSeconds(10));
        log.info("[PERFORMANCE_LOG] Time spent fetching initiativeId: {} ms", System.currentTimeMillis() - startTime);
        if (droolsRule==null){
            log.error("cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        return droolsRule.getInitiativeConfig();
    }
    //endregion

}
