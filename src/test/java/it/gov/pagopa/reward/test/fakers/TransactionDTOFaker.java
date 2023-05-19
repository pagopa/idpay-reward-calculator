package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
     * It will return an example of {@link TransactionDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static TransactionDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionDTO.TransactionDTOBuilder<?, ?> mockInstanceBuilder(Integer bias) {
        LocalDate trxDate = LocalDate.of(2022, getRandomPositiveNumber(bias, 11) + 1, getRandomPositiveNumber(bias, 27)+1);
        LocalTime trxTime = LocalTime.of(getRandomPositiveNumber(bias, 23), getRandomPositiveNumber(bias, 59), getRandomPositiveNumber(bias, 59));
        LocalDateTime trxDateTime = LocalDateTime.of(trxDate, trxTime);

        return TransactionDTO.builder()
                .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
                .acquirerCode("ACQUIRERCODE%d".formatted(bias))
                .trxDate(OffsetDateTime.of(
                        trxDateTime,
                        CommonConstants.ZONEID.getRules().getOffset(trxDateTime)
                ))
                .hpan("HPAN%d".formatted(bias))
                .operationType("00")
                .circuitType("CIRCUITTYPE%d".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
                .correlationId("CORRELATIONID%d".formatted(bias))
                .amount(BigDecimal.valueOf(getRandomPositiveNumber(bias, 200_00)).add(BigDecimal.valueOf(100)))
                .amountCents(null)
                .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
                .mcc("MCC%d".formatted(bias))
                .acquirerId("ACQUIRERID%d".formatted(bias))
                .merchantId("MERCHANTID%d".formatted(bias))
                .terminalId("TERMINALID%d".formatted(bias))
                .bin("BIN%d".formatted(bias))
                .senderCode("SENDERCODE%d".formatted(bias))
                .fiscalCode("FISCALCODE%d".formatted(bias))
                .vat("VAT%d".formatted(bias))
                .posType("POSTYPE%d".formatted(bias))
                .par("PAR%d".formatted(bias))
                .userId("USERID%d".formatted(bias))
                .maskedPan("MASKEDPAN%d".formatted(bias))
                .brandLogo("BRANDLOGO%d".formatted(bias))
                .brand("BRAND%d".formatted(bias))
                .channel(RewardConstants.TRX_CHANNEL_RTD)
                .ruleEngineTopicPartition(0)
                .ruleEngineTopicOffset(-1L);
    }
}
