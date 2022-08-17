package it.gov.pagopa.reward.service.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RewardRuleMediatorServiceImpl implements RewardRuleMediatorService {

    private final Duration rewardRulesBuildDelay;

    private final RewardRule2DroolsRuleService rewardRule2DroolsRuleService;
    private final DroolsRuleRepository droolsRuleRepository;
    private final KieContainerBuilderService kieContainerBuilderService;
    private final RewardContextHolderService rewardContextHolderService;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    public RewardRuleMediatorServiceImpl(@Value("${app.reward-rule.build-delay-duration}") String rewardRulesBuildDelay, RewardRule2DroolsRuleService rewardRule2DroolsRuleService, DroolsRuleRepository droolsRuleRepository, KieContainerBuilderService kieContainerBuilderService, RewardContextHolderService rewardContextHolderService, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.rewardRulesBuildDelay=Duration.parse(rewardRulesBuildDelay);
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
                .flatMap(this::execute)
                .buffer(rewardRulesBuildDelay)
                .flatMap(r -> kieContainerBuilderService.buildAll())
                .subscribe(rewardContextHolderService::setRewardRulesKieContainer);
    }

    private Mono<DroolsRule> execute(Message<String> message){
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .map(rewardRule2DroolsRuleService)
                .flatMap(droolsRuleRepository::save)
                .map(i -> {
                    rewardContextHolderService.setInitiativeConfig(i.getInitiativeConfig());
                    return i;})

                .onErrorResume(e->{
                    errorNotifierService.notifyRewardRuleBuilder(message, "An error occurred handling initiative", true, e);
                    return Mono.empty();
                });
    }

    private InitiativeReward2BuildDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyRewardRuleBuilder(message, "Unexpected JSON", true, e));
    }
}
