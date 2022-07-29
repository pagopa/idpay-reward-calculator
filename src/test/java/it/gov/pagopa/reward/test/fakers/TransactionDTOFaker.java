package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;

import java.math.BigDecimal;
import java.time.*;
import java.util.Locale;
import java.util.Random;

public class TransactionDTOFaker {
    private TransactionDTOFaker() {
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
     * It will return an example of {@link RewardGroupsDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static TransactionDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionDTO.TransactionDTOBuilder<?, ?> mockInstanceBuilder(Integer bias) {
        LocalDate trxDate = LocalDate.of(2022, getRandomPositiveNumber(bias, 11) + 1, getRandomPositiveNumber(bias, 27)+1);
        LocalTime trxTime = LocalTime.of(getRandomPositiveNumber(bias, 23), getRandomPositiveNumber(bias, 59), getRandomPositiveNumber(bias, 59));
        LocalDateTime trxDateTime = LocalDateTime.of(trxDate, trxTime);

        return TransactionDTO.builder()
                .idTrxAcquirer("IDTRXACQUIRER%s".formatted(bias))
                .acquirerCode("ACQUIRERCODE%s".formatted(bias))
                .trxDate(OffsetDateTime.of(
                        trxDateTime,
                        ZoneId.of("Europe/Rome").getRules().getOffset(trxDateTime)
                ))
                .hpan("HPAN%s".formatted(bias))
                .operationType("OPERATIONTYPE%s".formatted(bias))
                .circuitType("CIRCUITTYPE%s".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%s".formatted(bias))
                .correlationId("CORRELATIONID%s".formatted(bias))
                .amount(BigDecimal.valueOf(getRandomPositiveNumber(bias, 200)))
                .amountCurrency("AMOUNTCURRENCY%s".formatted(bias))
                .mcc("MCC%s".formatted(bias))
                .acquirerId("ACQUIRERID%s".formatted(bias))
                .merchantId("MERCHANTID%s".formatted(bias))
                .terminalId("TERMINALID%s".formatted(bias))
                .bin("BIN%s".formatted(bias))
                .senderCode("SENDERCODE%s".formatted(bias))
                .fiscalCode("FISCALCODE%s".formatted(bias))
                .vat("VAT%s".formatted(bias))
                .posType("POSTYPE%s".formatted(bias))
                .par("PAR%s".formatted(bias));
    }
}
