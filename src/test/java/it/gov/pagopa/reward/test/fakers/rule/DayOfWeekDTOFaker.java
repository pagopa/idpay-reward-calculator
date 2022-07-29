package it.gov.pagopa.reward.test.fakers.rule;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.rule.trx.DayOfWeekDTO;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DayOfWeekDTOFaker {
    private DayOfWeekDTOFaker(){}

    private static final Random randomGenerator = new Random();

    private static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    private static int getRandomPositiveNumber(Integer bias) {return Math.abs(getRandom(bias).nextInt());}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /** It will return an example of {@link DayOfWeekDTO}. Providing a bias, it will return a pseudo-casual object */
    public static DayOfWeekDTO mockInstance(Integer bias){
        return new DayOfWeekDTO(
                IntStream.rangeClosed(0, bias%7)
                        .mapToObj(i->
                                DayOfWeekDTO.DayConfig.builder()
                                        .daysOfWeek(
                                                IntStream.rangeClosed(0, i)
                                                        .mapToObj(j-> DayOfWeek.values()[getRandomPositiveNumber(bias)%7])
                                                        .collect(Collectors.toCollection(TreeSet::new))
                                        )
                                        .intervals(
                                                IntStream.rangeClosed(0, i%3)
                                                        .mapToObj(j-> DayOfWeekDTO.Interval.builder()
                                                                .startTime(LocalTime.MIDNIGHT.plusHours(j* 7L).plusMinutes(getRandomPositiveNumber(bias)%60))
                                                                .endTime(LocalTime.MIDNIGHT.plusHours(j* 7L +2))
                                                                .build())
                                                        .collect(Collectors.toList())
                                        )
                                        .build()
                        )
                        .collect(Collectors.toList()));
    }
}
