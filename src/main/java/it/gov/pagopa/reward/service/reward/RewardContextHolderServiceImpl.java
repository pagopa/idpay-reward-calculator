package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.kie.api.KieBase;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
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

    private final boolean preCompileContainer;

    private KieBase kieBase;
    private byte[] kieBaseSerialized;

    public RewardContextHolderServiceImpl(
            KieContainerBuilderService kieContainerBuilderService,
            DroolsRuleRepository droolsRuleRepository,
            ApplicationEventPublisher applicationEventPublisher,
            @Autowired(required = false) ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate,
            @Value("${spring.redis.enabled}") boolean isRedisCacheEnabled,
            @Value("${app.reward-rule.pre-compile}") boolean preCompileContainer) {
        this.kieContainerBuilderService =kieContainerBuilderService;
        this.droolsRuleRepository = droolsRuleRepository;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.isRedisCacheEnabled = isRedisCacheEnabled;
        this.preCompileContainer = preCompileContainer;

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
            kieBaseSerialized = SerializationUtils.serialize(kieBase);
            if (kieBaseSerialized != null) {
                reactiveRedisTemplate.opsForValue().set(REWARD_CONTEXT_HOLDER_CACHE_NAME, kieBaseSerialized).subscribe(x -> {
                    log.debug("Saving KieContainer in cache and compiling it");
                    compileKieBase();
                });
            } else {
                reactiveRedisTemplate.opsForValue().delete(REWARD_CONTEXT_HOLDER_CACHE_NAME).subscribe(x -> log.debug("Clearing KieContainer in cache"));
            }
        } else {
            if(kieBase!=null){
                compileKieBase();
            }
        }
    }

    private void compileKieBase(){
        if(preCompileContainer) {
            try {
                log.info("[DROOLS_CONTAINER_COMPILE] Starting KieContainer compile");
                long startTime = System.currentTimeMillis();
                TransactionDroolsDTO trx = new TransactionDroolsDTO();
                trx.setEffectiveAmount(BigDecimal.ONE);
                trx.setTrxChargeDate(OffsetDateTime.now());
                UserInitiativeCounters userCounters = new UserInitiativeCounters();
                userCounters.setInitiatives(new HashMap<>());

                List<Command<?>> cmds = new ArrayList<>();
                cmds.add(CommandFactory.newInsert(new RuleEngineConfig()));
                cmds.add(CommandFactory.newInsert(userCounters));
                cmds.add(CommandFactory.newInsert(trx));
                Arrays.stream(((KnowledgeBaseImpl) this.kieBase).getPackages()).flatMap(p -> p.getRules().stream()).map(r -> ((RuleImpl) r).getAgendaGroup())
                        .distinct().forEach(a -> cmds.add(new AgendaGroupSetFocusCommand(a)));
                StatelessKieSession session = this.kieBase.newStatelessKieSession();
                session.execute(CommandFactory.newBatchExecution(cmds));
                long endTime = System.currentTimeMillis();

                log.info("[DROOLS_CONTAINER_COMPILE] KieContainer instance compiled in {} ms", endTime - startTime);
            } catch (Exception e){
                log.warn("[DROOLS_CONTAINER_COMPILE] An error occurred while pre-compiling Drools rules", e);
            }
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
                            this.kieBase = (KieBase) SerializationUtils.deserialize(c);
                            this.kieBaseSerialized = c;
                            compileKieBase();
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
