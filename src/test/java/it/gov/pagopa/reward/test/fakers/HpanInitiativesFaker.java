package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.test.utils.TestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class HpanInitiativesFaker {
    private HpanInitiativesFaker(){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    public static HpanInitiatives mockInstance(Integer bias){
        HpanInitiatives out = mockInstanceWithoutInitiative(bias);

        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(String.format("INITIATIVE_%d",bias))
                .status("ACCEPTED")
                .activeTimeIntervals(new ArrayList<>()).build();

        LocalDateTime onboardedTime = LocalDateTime.now();
        ActiveTimeInterval interval1 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusYears(3L))
                .endInterval(onboardedTime.minusYears(2L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval1);

        ActiveTimeInterval interval2 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusMonths(5L).with(LocalTime.MIN).plusDays(1L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval2);

        out.setOnboardedInitiatives(List.of(onboardedInitiative));

        TestUtils.checkNotNullFields(out);
        return out;
    }

    public static HpanInitiatives mockInstanceWithoutInitiative(Integer bias){
        HpanInitiatives out = new HpanInitiatives();

        out.setHpan("HPAN_%d".formatted(bias));
        out.setUserId("USERID_%d".formatted(bias));


        TestUtils.checkNotNullFields(out,"onboardedInitiatives");
        return out;
    }

    public static HpanInitiatives mockInstanceNotInActiveInterval(Integer bias){
        HpanInitiatives out = new HpanInitiatives();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setHpan(fakeValuesService.bothify("?????"));
        out.setUserId(fakeValuesService.bothify("?????"));

        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(String.format("INITIATIVE_%d",bias))
                .status("ACCEPTED")
                .activeTimeIntervals(new ArrayList<>()).build();

        LocalDateTime onboardedTime = LocalDateTime.now();
        ActiveTimeInterval interval1 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusYears(3L))
                .endInterval(onboardedTime.minusYears(2L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval1);

        ActiveTimeInterval interval2 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusYears(4L))
                .endInterval(onboardedTime.minusYears(1L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval2);

        out.setOnboardedInitiatives(List.of(onboardedInitiative));


        TestUtils.checkNotNullFields(out,"onboardedInitiatives");
        return out;
    }

    public static HpanInitiatives mockInstanceWithCloseIntervals(Integer bias){
        HpanInitiatives out = mockInstanceWithoutInitiative(bias);

        LocalDateTime onboardedTime = LocalDateTime.now();
        LocalDateTime lastEndInterval = onboardedTime.minusMonths(5L).with(LocalTime.MAX);

        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(String.format("INITIATIVE_%d",bias))
                .status("ACCEPTED")
                .activeTimeIntervals(new ArrayList<>())
                .lastEndInterval(lastEndInterval).build();

        ActiveTimeInterval interval1 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusYears(3L))
                .endInterval(onboardedTime.minusYears(2L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval1);

        ActiveTimeInterval interval2 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusYears(1L).with(LocalTime.MIN).plusDays(1L))
                .endInterval(lastEndInterval).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval2);

        List<OnboardedInitiative> onboardedInitiativeList = new ArrayList<>();
        onboardedInitiativeList.add(onboardedInitiative);
        out.setOnboardedInitiatives(onboardedInitiativeList);


        TestUtils.checkNotNullFields(out);
        return out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }


}
