package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.CitizenHpan;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.test.utils.TestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class CitizenHpanFaker {
    private CitizenHpanFaker (){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    public static CitizenHpan mockInstance(Integer bias){
        CitizenHpan out = new CitizenHpan();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setHpan(fakeValuesService.bothify("?????"));
        out.setUserId(fakeValuesService.bothify("?????"));

        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder().build();
        onboardedInitiative.setInitiativeId(String.format("INITIATIVE_%d",bias));

        LocalDateTime onboardedTime = LocalDateTime.now();
        ActiveTimeInterval interval1 = ActiveTimeInterval.builder().build();
        interval1.setStartInterval(onboardedTime);
        interval1.setEndInterval(onboardedTime.plusDays(2L));
        ActiveTimeInterval interval2 = ActiveTimeInterval.builder().build();
        interval2.setStartInterval(onboardedTime.plusDays(5L));
        onboardedInitiative.setActiveTimeIntervals(Arrays.asList(interval1,interval2));

        out.setOnboardedInitiatives(List.of(onboardedInitiative));

        TestUtils.checkNotNullFields(out);
        return out;
    }

    public static CitizenHpan mockInstanceWithoutInitiative(Integer bias){
        CitizenHpan out = new CitizenHpan();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setHpan(fakeValuesService.bothify("?????"));
        out.setUserId(fakeValuesService.bothify("?????"));


        TestUtils.checkNotNullFields(out,"onboardedInitiatives");
        return out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }
}
