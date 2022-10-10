package it.gov.pagopa.reward.service.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.BaseKafkaConsumer;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Service
public class RewardRuleMediatorServiceImpl extends BaseKafkaConsumer<InitiativeReward2BuildDTO, DroolsRule> implements RewardRuleMediatorService {

    private final Duration commitDelay;
    private final Duration rewardRulesBuildDelayMinusCommit;

    private final RewardRule2DroolsRuleService rewardRule2DroolsRuleService;
    private final DroolsRuleRepository droolsRuleRepository;
    private final KieContainerBuilderService kieContainerBuilderService;
    private final RewardContextHolderService rewardContextHolderService;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public RewardRuleMediatorServiceImpl(
            @Value("${spring.cloud.stream.kafka.bindings.rewardRuleConsumer-in-0.consumer.ackTime}") Long commitMillis,
            @Value("${app.reward-rule.build-delay-duration}") String rewardRulesBuildDelay,

            RewardRule2DroolsRuleService rewardRule2DroolsRuleService,
            DroolsRuleRepository droolsRuleRepository,
            KieContainerBuilderService kieContainerBuilderService,
            RewardContextHolderService rewardContextHolderService,
            ErrorNotifierService errorNotifierService,

            ObjectMapper objectMapper) {
        this.commitDelay = Duration.ofMillis(commitMillis);

        Duration rewardRulesBuildDelayDuration = Duration.parse(rewardRulesBuildDelay).minusMillis(commitMillis);
        rewardRulesBuildDelayMinusCommit = rewardRulesBuildDelayDuration.isNegative() ? Duration.ZERO : rewardRulesBuildDelayDuration;

        this.rewardRule2DroolsRuleService = rewardRule2DroolsRuleService;
        this.droolsRuleRepository = droolsRuleRepository;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.rewardContextHolderService = rewardContextHolderService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(InitiativeReward2BuildDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<DroolsRule>> afterCommits2subscribe) {
        afterCommits2subscribe
                .buffer(rewardRulesBuildDelayMinusCommit)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(rewardContextHolderService::setRewardRulesKieBase);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> errorNotifierService.notifyRewardRuleBuilder(message, "[REWARD_RULE_BUILD] Unexpected JSON", true, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        errorNotifierService.notifyRewardRuleBuilder(message, "[REWARD_RULE_BUILD] An error occurred handling initiative", true, e);
    }

    @Override
    protected Mono<DroolsRule> execute(InitiativeReward2BuildDTO payload, Message<String> message) {
        return Mono.just(payload)
                .map(rewardRule2DroolsRuleService)
                .flatMap(droolsRuleRepository::save)
                .map(i -> {
                    rewardContextHolderService.setInitiativeConfig(i.getInitiativeConfig());
                    return i;
                });
    }
}
