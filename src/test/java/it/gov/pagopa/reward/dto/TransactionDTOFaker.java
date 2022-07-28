package it.gov.pagopa.reward.dto;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
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
        LocalDate trxDate = LocalDate.of(2022, getRandomPositiveNumber(bias, 11)+1, getRandomPositiveNumber(bias, 28));
        LocalTime trxTime = LocalTime.of(getRandomPositiveNumber(bias, 23), getRandomPositiveNumber(bias, 59), getRandomPositiveNumber(bias, 59));
        LocalDateTime trxDateTime = LocalDateTime.of(trxDate, trxTime);

        TransactionDTO out = new TransactionDTO();
        out.setIdTrxAcquirer("IDTRXACQUIRER%s".formatted(bias));
        out.setAcquirerCode("ACQUIRERCODE%s".formatted(bias));
        out.setTrxDate(OffsetDateTime.of(
                trxDateTime,
                ZoneId.of("Europe/Rome").getRules().getOffset(trxDateTime)
                ));
        out.setHpan("HPAN%s".formatted(bias));
        out.setOperationType("OPERATIONTYPE%s".formatted(bias));
        out.setCircuitType("CIRCUITTYPE%s".formatted(bias));
        out.setIdTrxIssuer("IDTRXISSUER%s".formatted(bias));
        out.setCorrelationId("CORRELATIONID%s".formatted(bias));
        out.setAmount(BigDecimal.valueOf(getRandomPositiveNumber(bias, 200)));
        out.setAmountCurrency("AMOUNTCURRENCY%s".formatted(bias));
        out.setMcc("MCC%s".formatted(bias));
        out.setAcquirerId("ACQUIRERID%s".formatted(bias));
        out.setMerchantId("MERCHANTID%s".formatted(bias));
        out.setTerminalId("TERMINALID%s".formatted(bias));
        out.setBin("BIN%s".formatted(bias));
        out.setSenderCode("SENDERCODE%s".formatted(bias));
        out.setFiscalCode("FISCALCODE%s".formatted(bias));
        out.setVat("VAT%s".formatted(bias));
        out.setPosType("POSTYPE%s".formatted(bias));
        out.setPar("PAR%s".formatted(bias));
        return out;
    }
}
