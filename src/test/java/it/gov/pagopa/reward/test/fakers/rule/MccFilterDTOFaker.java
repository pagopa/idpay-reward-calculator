package it.gov.pagopa.reward.test.fakers.rule;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class MccFilterDTOFaker {
    private MccFilterDTOFaker(){}

    private static final Random randomGenerator = new Random();

    private static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /** It will return an example of {@link MccFilterDTO}. Providing a bias, it will return a pseudo-casual object */
    public static MccFilterDTO mockInstance(Integer bias){
        bias = ObjectUtils.firstNonNull(bias, getRandom(null).nextInt());
        return MccFilterDTO.builder().values(new TreeSet<>(Set.of("MCC_%d".formatted(bias), getFakeValuesService(bias).bothify("####")))).build();
    }
}
