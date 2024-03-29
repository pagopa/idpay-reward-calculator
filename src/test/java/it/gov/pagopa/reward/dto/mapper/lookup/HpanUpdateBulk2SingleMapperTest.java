package it.gov.pagopa.reward.dto.mapper.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

class HpanUpdateBulk2SingleMapperTest {

    @Test
    void apply() {
        // Given
        HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper = new HpanUpdateBulk2SingleMapper();

        LocalDateTime evaluationDate = LocalDateTime.now().with(LocalTime.MIN).plusDays(1L);

        PaymentMethodInfoDTO infoHpan = PaymentMethodInfoDTO.builder()
                .hpan("HPAN_1")
                .maskedPan("MASKEDPAN_1")
                .brandLogo("BRANDLOGO_1")
                .brand("BRAND_1").build();

        HpanInitiativeBulkDTO hpanInitiativeBulkDTO = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .infoList(List.of(infoHpan))
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now()).build();

        // When
        HpanUpdateEvaluateDTO result = hpanUpdateBulk2SingleMapper.apply(hpanInitiativeBulkDTO, hpanInitiativeBulkDTO.getInfoList().get(0), evaluationDate);
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "onboardedInitiative");
        Assertions.assertEquals("HPAN_1", result.getHpan());
        Assertions.assertEquals("MASKEDPAN_1", result.getMaskedPan());
        Assertions.assertEquals("BRANDLOGO_1", result.getBrandLogo());
        Assertions.assertEquals("BRAND_1", result.getBrand());
        Assertions.assertEquals("USERID", result.getUserId());
        Assertions.assertEquals("INITIATIVEID", result.getInitiativeId());
        Assertions.assertEquals(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT, result.getOperationType());
        Assertions.assertEquals(evaluationDate, result.getEvaluationDate());
    }
}