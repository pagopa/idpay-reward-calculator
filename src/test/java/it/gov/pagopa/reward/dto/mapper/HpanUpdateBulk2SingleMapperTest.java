package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class HpanUpdateBulk2SingleMapperTest {

    @Test
    void apply() {
        // Given
        HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper = new HpanUpdateBulk2SingleMapper();

        LocalDateTime now = LocalDateTime.now();

        HpanInitiativeBulkDTO hpanInitiativeBulkDTO = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .hpanList(List.of("HPAN_1", "HPAN_2"))
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT)
                .operationDate(now).build();

        // When
        HpanInitiativeDTO result = hpanUpdateBulk2SingleMapper.apply(hpanInitiativeBulkDTO, "HPAN_1");
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertEquals("HPAN_1", result.getHpan());
        Assertions.assertEquals("USERID", result.getUserId());
        Assertions.assertEquals("INITIATIVEID", result.getInitiativeId());
        Assertions.assertEquals(HpanInitiativeConstants.ADD_INSTRUMENT, result.getOperationType());
        Assertions.assertEquals(now, result.getOperationDate());
    }
}