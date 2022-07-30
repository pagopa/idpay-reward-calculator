package it.gov.pagopa.reward;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
public class IdPayRewardCalculatorApplication {

    public static void main(String[] args) {
        Locale.setDefault(Locale.ITALY);
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
        SpringApplication.run(IdPayRewardCalculatorApplication.class, args);
    }

}
