package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import java.util.Locale;

public class HpanInitiativeDTOFaker {

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    public static HpanInitiativeDTO mockInstance(Integer bias){
        HpanInitiativeDTO out = HpanInitiativeDTO.builder().build();

        out.setHpan("HPAN_%d".formatted(bias));
        out.setUserId("USERID_%d".formatted(bias));
        out.setInitiativeId("INITIATIVEID_%d".formatted(bias));
        return out;
    }

}
