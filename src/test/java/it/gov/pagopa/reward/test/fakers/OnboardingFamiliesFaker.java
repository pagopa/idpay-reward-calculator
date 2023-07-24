package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.model.OnboardingFamilies;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Random;

public class OnboardingFamiliesFaker {
    private OnboardingFamiliesFaker() {
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
     * It will return an example of {@link OnboardingFamilies}. Providing a bias, it will return a pseudo-casual object
     */
    public static OnboardingFamilies mockInstance(Integer bias) {
        return mockInstanceBuilder(bias).build();
    }

    public static OnboardingFamilies.OnboardingFamiliesBuilder mockInstanceBuilder(Integer bias) {
        String familyId = "FAM.ID_%d".formatted(bias);
        String initiativeId = "INITIATIVEID_%d".formatted(bias);
        return OnboardingFamilies.builder()
                .id(OnboardingFamilies.buildId(familyId, initiativeId))
                .familyId(familyId)
                .initiativeId(initiativeId)
                .createDate(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
