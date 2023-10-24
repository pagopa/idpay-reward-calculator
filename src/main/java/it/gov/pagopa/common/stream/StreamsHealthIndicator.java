package it.gov.pagopa.common.stream;

import lombok.NonNull;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@GlobalChannelInterceptor
public class StreamsHealthIndicator extends AbstractHealthIndicator implements ChannelInterceptor {

    private final ApplicationContext applicationContext;

    private final Set<String> disconnectedSubscribers = new HashSet<>();

    public StreamsHealthIndicator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Map<String, Integer> publisherSubscriptionCounts = applicationContext.getBeansOfType(DirectWithAttributesChannel.class).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSubscriberCount()));

        if(publisherSubscriptionCounts.values().stream().anyMatch(subscriptions -> subscriptions == 0) || !disconnectedSubscribers.isEmpty()){
            publisherSubscriptionCounts = new HashMap<>(publisherSubscriptionCounts);
            publisherSubscriptionCounts.putAll(disconnectedSubscribers.stream().collect(Collectors.toMap(Function.identity(), x -> 0)));

            builder.down();
        } else {
            builder.up();
        }
        builder.withDetails(publisherSubscriptionCounts);
    }

    /** error message printed when reactive stream ends caused by unhandled error, breaking the Spring Stream */
    public static final String SUBSCRIBER_DISCONNECTED_SUFFIX = "'] doesn't have subscribers to accept messages";
    @Override
    public void afterSendCompletion(@NonNull Message<?> message, @NonNull MessageChannel channel, boolean sent, Exception ex) {
        if(ex instanceof IllegalStateException && ex.getMessage().endsWith(SUBSCRIBER_DISCONNECTED_SUFFIX)){
            String channelName = ex.getMessage().substring(11, ex.getMessage().indexOf(SUBSCRIBER_DISCONNECTED_SUFFIX));
            if(!disconnectedSubscribers.contains(channelName)){
                synchronized (disconnectedSubscribers){
                    disconnectedSubscribers.add(channelName);
                }
            }
        }
    }
}
