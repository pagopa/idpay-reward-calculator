package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.enums.OnboardingStatus;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.UserInitiatives;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class UserInitiativesFaker {
    private UserInitiativesFaker(){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    public static UserInitiatives mockInstance(Integer bias){
        return mockInstance(bias, String.format("INITIATIVE_%d",bias) );
    }

    public static UserInitiatives mockInstance(Integer bias, String initiativeId){
        UserInitiatives out = mockInstanceWithoutInitiative(bias);

        LocalDateTime onboardedTime = LocalDateTime.now().with(LocalTime.MIN);
        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(initiativeId)
                .status(OnboardingStatus.ACTIVE)
                .updateDate(onboardedTime)
                .activeTimeIntervals(new ArrayList<>()).build();

        ActiveTimeInterval interval1 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusYears(3L))
                .endInterval(onboardedTime.minusYears(2L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval1);

        ActiveTimeInterval interval2 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusMonths(5L).plusDays(1L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval2);

        out.setOnboardedInitiatives(List.of(onboardedInitiative));

        TestUtils.checkNotNullFields(out);
        return out;
    }

    public static UserInitiatives mockInstanceWithoutInitiative(Integer bias){
        UserInitiatives out = new UserInitiatives();
        out.setUserId("USERID_%d".formatted(bias));
        TestUtils.checkNotNullFields(out,"onboardedInitiatives");
        return out;
    }

    public static UserInitiatives mockInstanceNotInActiveInterval(Integer bias){
        UserInitiatives out = new UserInitiatives();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setUserId(fakeValuesService.bothify("?????"));

        LocalDateTime onboardedTime = LocalDateTime.now();
        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(String.format("INITIATIVE_%d",bias))
                .status(OnboardingStatus.ACTIVE)
                .updateDate(onboardedTime)
                .activeTimeIntervals(new ArrayList<>()).build();

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

    public static UserInitiatives mockInstanceWithCloseIntervals(Integer bias){
        return mockInstanceWithCloseIntervals(bias, String.format("INITIATIVE_%d",bias));
    }

    public static UserInitiatives mockInstanceWithCloseIntervals(Integer bias, String initiative){
        UserInitiatives out = mockInstanceWithoutInitiative(bias);

        LocalDateTime onboardedTime = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime lastEndInterval = onboardedTime.minusMonths(5L);

        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(initiative)
                .status(OnboardingStatus.ACTIVE)
                .updateDate(onboardedTime)
                .activeTimeIntervals(new ArrayList<>())
                .lastEndInterval(lastEndInterval).build();

        ActiveTimeInterval interval1 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusYears(3L))
                .endInterval(onboardedTime.minusYears(2L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval1);

        ActiveTimeInterval interval2 = ActiveTimeInterval.builder().startInterval(onboardedTime.minusYears(1L).plusDays(1L))
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
