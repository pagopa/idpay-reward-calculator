package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;

import java.util.Locale;
import java.util.Random;

public class SynchronousTransactionResponseDTOFaker {

    private SynchronousTransactionResponseDTOFaker() {
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
     * It will return an example of {@link SynchronousTransactionResponseDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static SynchronousTransactionResponseDTO mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static SynchronousTransactionResponseDTO.SynchronousTransactionResponseDTOBuilder mockInstanceBuilder(Integer bias) {
        return SynchronousTransactionResponseDTO.builder()
                .transactionId("TRANSACTIONID%d".formatted(bias))
                .initiativeId("INITIATIVEID%d".formatted(bias))
                .userId("USERID%d".formatted(bias));
    }


}
