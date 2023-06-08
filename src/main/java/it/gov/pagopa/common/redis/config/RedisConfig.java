package it.gov.pagopa.common.redis.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnProperty(prefix = "spring.redis", name = "enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        RedisSerializationContext<String, byte[]> context = RedisSerializationContext.<String, byte[]>newSerializationContext(new StringRedisSerializer())
                        .value(RedisSerializationContext.SerializationPair.byteArray())
                        .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

}
