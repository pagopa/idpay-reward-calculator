package it.gov.pagopa.common.redis.config;

import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import it.gov.pagopa.common.utils.TestUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

@TestConfiguration
@AutoConfigureSingleInstanceMongodb
public class EmbeddedRedisTestConfiguration {
    private final RedisServer redisServer;

    public EmbeddedRedisTestConfiguration() {
        if(TestUtils.availableLocalPort(8080)){
            this.redisServer = new RedisServer(8080);
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
