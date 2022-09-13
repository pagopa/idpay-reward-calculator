package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import java.util.Locale;

public class HpanInitiativeDTOFaker {
    public static HpanInitiativeDTO.HpanInitiativeDTOBuilder mockInstanceBuilder(Integer bias){
        return HpanInitiativeDTO.builder()
                .hpan("HPAN_%d".formatted(bias))
                .userId("USERID_%d".formatted(bias))
                .initiativeId("INITIATIVE_%d".formatted(bias));
    }
    public static HpanInitiativeDTO mockInstance(Integer bias){
        mockInstanceBuilder(bias).build();
        HpanInitiativeDTO out = mockInstanceBuilder(bias).build();
        return out;
    }

}
