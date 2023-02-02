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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@Slf4j
public class RewardContextHolderServiceImpl implements RewardContextHolderService {

    public static final String REWARD_CONTEXT_HOLDER_CACHE_NAME = "reward_rule";

    private final KieContainerBuilderService kieContainerBuilderService;
    private final Map<String, InitiativeConfig> initiativeId2Config=new ConcurrentHashMap<>();
    private final DroolsRuleRepository droolsRuleRepository;
    private final ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate;
    private final boolean isRedisCacheEnabled;

    private final boolean preLoadContainer;

    private KieBase kieBase;
    private byte[] kieBaseSerialized;

    public RewardContextHolderServiceImpl(
            KieContainerBuilderService kieContainerBuilderService,
            DroolsRuleRepository droolsRuleRepository,
            ApplicationEventPublisher applicationEventPublisher,
            @Autowired(required = false) ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate,
            @Value("${spring.redis.enabled}") boolean isRedisCacheEnabled,
            @Value("${app.reward-rule.pre-load}") boolean preLoadContainer) {
        this.kieContainerBuilderService =kieContainerBuilderService;
        this.droolsRuleRepository = droolsRuleRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.isRedisCacheEnabled = isRedisCacheEnabled;
        this.preLoadContainer = preLoadContainer;

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
    public void setRewardRulesKieBase(KieBase newKieBase) {
        preLoadKieBase(newKieBase);
        this.kieBase = newKieBase;

        if (isRedisCacheEnabled) {
            kieBaseSerialized = SerializationUtils.serialize(newKieBase);
            if (kieBaseSerialized != null) {
                reactiveRedisTemplate.opsForValue().set(REWARD_CONTEXT_HOLDER_CACHE_NAME, kieBaseSerialized).subscribe(x -> log.info("KieContainer build and stored in cache"));
            } else {
                reactiveRedisTemplate.opsForValue().delete(REWARD_CONTEXT_HOLDER_CACHE_NAME).subscribe(x -> log.info("KieContainer removed from the cache"));
            }
        }
    }

    private void preLoadKieBase(KieBase kieBase){
        if(preLoadContainer) {
            kieContainerBuilderService.preLoadKieBase(kieBase);
        }
    }

    @Scheduled(initialDelayString = "${app.reward-rule.cache.refresh-ms-rate}", fixedRateString = "${app.reward-rule.cache.refresh-ms-rate}")
    public void refreshKieContainer(){
        refreshKieContainer(x -> log.trace("Refreshed KieContainer"));
    }

    public void refreshKieContainer(Consumer<? super KieBase> subscriber) {
        if (isRedisCacheEnabled) {
            reactiveRedisTemplate.opsForValue().get(REWARD_CONTEXT_HOLDER_CACHE_NAME)
                    .map(c -> {
                        if(!Arrays.equals(c, kieBaseSerialized)){
                            this.kieBaseSerialized = c;
                            KieBase newKieBase = (KieBase) SerializationUtils.deserialize(c);
                            preLoadKieBase(newKieBase);
                            this.kieBase=newKieBase;
                        }
                        return this.kieBase;
                    })
                    .switchIfEmpty(refreshKieContainerCacheMiss())
                    .subscribe(subscriber);
        } else {
            refreshKieContainerCacheMiss().subscribe(subscriber);
        }
    }

    private Mono<KieBase> refreshKieContainerCacheMiss() {
        final Flux<DroolsRule> droolsRuleFlux = Mono.defer(() -> {
            log.info("[REWARD_RULE_BUILD] Refreshing KieContainer");
            initiativeId2Config.clear();
            return Mono.empty();
        }).thenMany(droolsRuleRepository.findAll().doOnNext(dr -> setInitiativeConfig(dr.getInitiativeConfig())));
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
        log.info("[CACHE_MISS] [PERFORMANCE_LOG] Time spent fetching initiativeId: {} ms", System.currentTimeMillis() - startTime);
        if (droolsRule==null){
            log.error("[REWARD_CONTEXT] cannot find initiative having id %s".formatted(initiativeId));
            return null;
        }
        return droolsRule.getInitiativeConfig();
    }
    //endregion
}
