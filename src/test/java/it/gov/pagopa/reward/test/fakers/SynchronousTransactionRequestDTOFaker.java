package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.enums.OperationType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Random;

public class SynchronousTransactionRequestDTOFaker {

    private SynchronousTransactionRequestDTOFaker() {
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
     * It will return an example of {@link SynchronousTransactionRequestDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static SynchronousTransactionRequestDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static SynchronousTransactionRequestDTO.SynchronousTransactionRequestDTOBuilder mockInstanceBuilder(Integer bias) {
        OffsetDateTime offsetDateTimeNow = OffsetDateTime.now(ZoneOffset.UTC);
        return SynchronousTransactionRequestDTO.builder()
                .transactionId("TRANSACTIONID%d".formatted(bias))
                .channel("SYNCPAYMENTCHANNEL%d".formatted(bias))
                .userId("USERID%d".formatted(bias))
                .merchantId("MERCHANTID%d".formatted(bias))
                .senderCode("SENDERCODE%d".formatted(bias))
                .merchantFiscalCode("MERCHANTFISCALCODE%d".formatted(bias))
                .vat("VAT%d".formatted(bias))
                .trxDate(offsetDateTimeNow)
                .amountCents(1_000L)
                .amountCurrency("AMOUNTCURRENCY%d".formatted(bias))
                .mcc("MCC%d".formatted(bias))
                .acquirerCode("ACQUIRERCODE%d".formatted(bias))
                .acquirerId("ACQUIRERID%d".formatted(bias))
                .idTrxAcquirer("IDTRXACQUIRER%d".formatted(bias))
                .idTrxIssuer("IDTRXISSUER%d".formatted(bias))
                .correlationId("CORRELATIONID%d".formatted(bias))
                .operationType(OperationType.CHARGE)
                .trxChargeDate(offsetDateTimeNow)
                .channel("SYNCPAYMENTCHANNEL%d".formatted(bias));
    }


}
