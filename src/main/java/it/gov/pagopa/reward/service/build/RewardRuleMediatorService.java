package it.gov.pagopa.reward.service.build;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component given an initiative will:
 * <ol>
 *     <li>translate it into drools syntax</li>
 *     <li>store it inside DB</li>
 *     <li>update the kieContainer</li>
 *     <li>notify the new kieContainer</li>
 * </ol>
 * */
public interface RewardRuleMediatorService {
    void execute(Flux<Message<String>> initiativeRewardRuleDTOFlux);
}
