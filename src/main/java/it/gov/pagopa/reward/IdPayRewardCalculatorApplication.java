package it.gov.pagopa.reward;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class IdPayRewardCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdPayRewardCalculatorApplication.class, args);
    }

}
