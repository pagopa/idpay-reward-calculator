package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.test.utils.TestUtils;

import java.time.LocalDateTime;
import java.util.*;

public final class HpanInitiativesFaker {
    private HpanInitiativesFaker(){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    public static HpanInitiatives mockInstance(Integer bias){
        HpanInitiatives out = new HpanInitiatives();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setHpan(fakeValuesService.bothify("?????"));
        out.setUserId(fakeValuesService.bothify("?????"));

        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(String.format("INITIATIVE_%d",bias))
                .status("ACCEPTED")
                .activeTimeIntervals(new ArrayList<>()).build();

        LocalDateTime onboardedTime = LocalDateTime.now();
        ActiveTimeInterval interval1 = ActiveTimeInterval.builder().startInterval(onboardedTime)
                .endInterval(onboardedTime.plusDays(2L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval1);

        ActiveTimeInterval interval2 = ActiveTimeInterval.builder().startInterval(onboardedTime.plusDays(5L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval2);

        out.setOnboardedInitiatives(List.of(onboardedInitiative));

        TestUtils.checkNotNullFields(out);
        return out;
    }

    public static HpanInitiatives mockInstanceWithoutInitiative(Integer bias){
        HpanInitiatives out = new HpanInitiatives();

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