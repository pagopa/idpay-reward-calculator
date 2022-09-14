package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;

import java.math.BigDecimal;
import java.time.*;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class TransactionProcessedFaker {
    private TransactionProcessedFaker() {
    }

    private static final Random randomGenerator = new Random();

    private static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    private static int getRandomPositiveNumber(Integer bias) {
        return Math.abs(getRandom(bias).nextInt());
    }

    private static int getRandomPositiveNumber(Integer bias, int bound) {
        return Math.abs(getRandom(bias).nextInt(bound));
    }

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /**
     * It will return an example of {@link TransactionDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static TransactionProcessed mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionProcessed.TransactionProcessedBuilder mockInstanceBuilder(Integer bias) {
        LocalDate trxDate = LocalDate.of(2022, getRandomPositiveNumber(bias, 11) + 1, getRandomPositiveNumber(bias, 27)+1);
        LocalTime trxTime = LocalTime.of(getRandomPositiveNumber(bias, 23), getRandomPositiveNumber(bias, 59), getRandomPositiveNumber(bias, 59));
        LocalDateTime trxDateTime = LocalDateTime.of(trxDate, trxTime);

        Map<String, Reward> rewards = Map.of(
                "REWARDS%s".formatted(bias),
                new Reward(BigDecimal.valueOf(getRandomPositiveNumber(bias, 200)))
        );


        return TransactionProcessed.builder()
                .id("IDTRX%s".formatted(bias))
                .idTrxAcquirer("IDTRXACQUIRER%s".formatted(bias))
                .acquirerCode("ACQUIRERCODE%s".formatted(bias))
                .trxDate(trxDateTime)
                .operationType("OPERATIONTYPE%s".formatted(bias))
                .correlationId("CORRELATIONID%s".formatted(bias))
                .amount(BigDecimal.valueOf(getRandomPositiveNumber(bias, 200)))
                .acquirerId("ACQUIRERID%s".formatted(bias))
                .userId("USERID%s".formatted(bias))
                .rewards(rewards);
    }
}
