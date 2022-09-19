package it.gov.pagopa.reward.service.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
public class RewardRuleMediatorServiceImpl implements RewardRuleMediatorService {

    private final Duration commitDelay;
    private final Duration rewardRulesBuildDelayMinusCommit;

    private final RewardRule2DroolsRuleService rewardRule2DroolsRuleService;
    private final DroolsRuleRepository droolsRuleRepository;
    private final KieContainerBuilderService kieContainerBuilderService;
    private final RewardContextHolderService rewardContextHolderService;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    public RewardRuleMediatorServiceImpl(
            @Value("${spring.cloud.stream.kafka.bindings.rewardRuleConsumer-in-0.consumer.ackTime}") Long commitMillis,
            @Value("${app.reward-rule.build-delay-duration}") String rewardRulesBuildDelay,

            RewardRule2DroolsRuleService rewardRule2DroolsRuleService,
            DroolsRuleRepository droolsRuleRepository,
            KieContainerBuilderService kieContainerBuilderService,
            RewardContextHolderService rewardContextHolderService,
            ErrorNotifierService errorNotifierService,

            ObjectMapper objectMapper) {
        this.commitDelay=Duration.ofMillis(commitMillis);

        Duration rewardRulesBuildDelayDuration= Duration.parse(rewardRulesBuildDelay).minusMillis(commitMillis);
        rewardRulesBuildDelayMinusCommit = rewardRulesBuildDelayDuration.isNegative()? Duration.ZERO : rewardRulesBuildDelayDuration;

        this.rewardRule2DroolsRuleService = rewardRule2DroolsRuleService;
        this.droolsRuleRepository = droolsRuleRepository;
        this.kieContainerBuilderService = kieContainerBuilderService;
        this.rewardContextHolderService = rewardContextHolderService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(InitiativeReward2BuildDTO.class);
    }

    @Override
    public void execute(Flux<Message<String>> initiativeBeneficiaryRuleDTOFlux) {
        initiativeBeneficiaryRuleDTOFlux
                .flatMapSequential(this::execute)

                .buffer(commitDelay)
                .mapNotNull(p -> p.stream().map(ack2entity -> {
                    ack2entity.getKey().ifPresent(Acknowledgment::acknowledge);
                    return ack2entity.getValue();
                }))

                .buffer(rewardRulesBuildDelayMinusCommit)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(rewardContextHolderService::setRewardRulesKieContainer);
    }

    private Mono<Pair<Optional<Acknowledgment>, DroolsRule>> execute(Message<String> message){
        Optional<Acknowledgment> ackOpt = Optional.ofNullable(message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class));

        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .map(rewardRule2DroolsRuleService)
                .flatMap(droolsRuleRepository::save)
                .map(i -> {
                    rewardContextHolderService.setInitiativeConfig(i.getInitiativeConfig());
                    return Pair.of(ackOpt, i);
                })
                .defaultIfEmpty(Pair.of(ackOpt, null))

                .onErrorResume(e->{
                    errorNotifierService.notifyRewardRuleBuilder(message, "An error occurred handling initiative", true, e);
                    return Mono.empty();
                });
    }

    private InitiativeReward2BuildDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyRewardRuleBuilder(message, "Unexpected JSON", true, e));
    }
}
