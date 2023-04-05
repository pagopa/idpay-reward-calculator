package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.synchronous.TransactionSynchronousRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Random;

public class TransactionPreviewRequestFaker {

    private TransactionPreviewRequestFaker() {
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
     * It will return an example of {@link TransactionSynchronousRequest}. Providing a bias, it will return a pseudo-casual object
     */
    public static TransactionSynchronousRequest mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static TransactionSynchronousRequest.TransactionSynchronousRequestBuilder mockInstanceBuilder(Integer bias) {
        LocalDate trxDate = LocalDate.of(2022, getRandomPositiveNumber(bias, 11) + 1, getRandomPositiveNumber(bias, 27)+1);
        LocalTime trxTime = LocalTime.of(getRandomPositiveNumber(bias, 23), getRandomPositiveNumber(bias, 59), getRandomPositiveNumber(bias, 59));
        LocalDateTime trxDateTime = LocalDateTime.of(trxDate, trxTime);

        return TransactionSynchronousRequest.builder()
                .transactionId("TRANSACTIONID%d".formatted(bias))
                .userId("USERID%d".formatted(bias))
                .merchantId("MERCHANTID%d".formatted(bias))
                .senderCode("SENDERCODE%d".formatted(bias))
                .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
                .vat("VAT%d".formatted(bias))
                .trxDate(OffsetDateTime.now())
                .amount(BigDecimal.TEN)
                .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
                .mcc("MCC%d".formatted(bias))
                .acquirerCode("ACQUIRERCODE%d".formatted(bias))
                .acquirerId("ACQUIRERID%d".formatted(bias))
                .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%d".formatted(bias));
    }


}
