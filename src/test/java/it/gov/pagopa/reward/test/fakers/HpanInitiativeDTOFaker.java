package it.gov.pagopa.reward.test.fakers;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;

public class HpanInitiativeDTOFaker {
    public static HpanUpdateEvaluateDTO.HpanUpdateEvaluateDTOBuilder mockInstanceBuilder(Integer bias){
        return HpanUpdateEvaluateDTO.builder()
                .hpan("HPAN_%d".formatted(bias))
                .userId("USERID_%d".formatted(bias))
                .initiativeId("INITIATIVE_%d".formatted(bias));
    }
    public static HpanUpdateEvaluateDTO mockInstance(Integer bias){
        mockInstanceBuilder(bias).build();
        HpanUpdateEvaluateDTO out = mockInstanceBuilder(bias).build();
        return out;
    }

}
