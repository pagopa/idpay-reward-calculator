package it.gov.pagopa.reward.connector.event.consumer;

import it.gov.pagopa.reward.service.build.RewardRuleMediatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

class RewardRuleConsumerConfigTest {

    @Test
    void rewardRuleConsumer() {
        RewardRuleMediatorService rewardRuleMediatorServiceMock = Mockito.mock(RewardRuleMediatorService.class);
        RewardRuleConsumerConfig consumerConfig = new RewardRuleConsumerConfig();

        Consumer<Flux<Message<String>>> result = consumerConfig.rewardRuleConsumer(rewardRuleMediatorServiceMock);

        Assertions.assertNotNull(result);

    }
}