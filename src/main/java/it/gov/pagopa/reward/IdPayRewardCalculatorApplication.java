package it.gov.pagopa.reward;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
public class IdPayRewardCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdPayRewardCalculatorApplication.class, args);
    }

}
