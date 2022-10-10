package it.gov.pagopa.reward.test.fakers;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;

import java.util.List;

public class HpanInitiativeBulkDTOFaker {
    public static HpanInitiativeBulkDTO.HpanInitiativeBulkDTOBuilder mockInstanceBuilder(Integer bias){
        PaymentMethodInfoDTO infoHpan = PaymentMethodInfoDTO.builder()
                .hpan("HPAN_%d".formatted(bias))
                .maskedPan("MASKEDPAN_%d".formatted(bias))
                .brandLogo("BRANDLOGO_%d".formatted(bias)).build();

        return HpanInitiativeBulkDTO.builder()
                .infoList(List.of(infoHpan))
                .userId("USERID_%d".formatted(bias))
                .initiativeId("INITIATIVE_%d".formatted(bias));
    }
}
