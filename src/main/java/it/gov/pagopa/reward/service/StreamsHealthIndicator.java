package it.gov.pagopa.reward.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StreamsHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        final Map<String, Integer> subscriptionCounts = applicationContext.getBeansOfType(DirectWithAttributesChannel.class).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSubscriberCount()));

        if(subscriptionCounts.values().stream().anyMatch(subscriptions -> subscriptions == 0)){
            builder.down();
        } else {
            builder.up();
        }
        builder.withDetails(subscriptionCounts);
    }

}
