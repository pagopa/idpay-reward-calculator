package it.gov.pagopa.reward.test.fakers;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;

import java.util.List;

public class HpanInitiativeBulkDTOFaker {
    public static HpanInitiativeBulkDTO.HpanInitiativeBulkDTOBuilder mockInstanceBuilder(Integer bias){
        return HpanInitiativeBulkDTO.builder()
                .hpanList(List.of("HPAN_%d".formatted(bias)))
                .userId("USERID_%d".formatted(bias))
                .initiativeId("INITIATIVE_%d".formatted(bias));
    }
}
