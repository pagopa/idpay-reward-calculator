package it.gov.pagopa.common.redis.configuration;

import it.gov.pagopa.common.utils.TestUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

@TestConfiguration
public class EmbeddedRedisTestConfiguration {
    private final RedisServer redisServer;

    public EmbeddedRedisTestConfiguration(RedisProperties redisProperties) {
        if(TestUtils.availableLocalPort(redisProperties.getPort())){
            this.redisServer = new RedisServer(redisProperties.getPort());
        } else {
            this.redisServer=null;
        }
    }

    @PostConstruct
    public void postConstruct() {
        if(redisServer!=null){
            redisServer.start();
        }
    }

    @PreDestroy
    public void preDestroy() {
        if(redisServer!=null){
            redisServer.stop();
        }
    }
}
