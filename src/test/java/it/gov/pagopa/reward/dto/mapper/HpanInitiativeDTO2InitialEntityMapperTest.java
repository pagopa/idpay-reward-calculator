package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class HpanInitiativeDTO2InitialEntityMapperTest {

    @Test
    void addedApply() {
        // Given
        HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper = new HpanInitiativeDTO2InitialEntityMapper();

        HpanInitiativeDTO hpanInitiativeDTO = HpanInitiativeDTOFaker.mockInstanceBuilder(1)
                .operationDate(LocalDateTime.now())
                .operationType(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name())
                .build();

        // When
        HpanInitiatives result = hpanInitiativeDTO2InitialEntityMapper.apply(hpanInitiativeDTO);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "onboardedInitiatives");
    }

    @Test
    void deletedApply() {
        // Given
        HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper = new HpanInitiativeDTO2InitialEntityMapper();

        HpanInitiativeDTO hpanInitiativeDTO = HpanInitiativeDTOFaker.mockInstanceBuilder(1)
                .operationDate(LocalDateTime.now())
                .operationType(HpanInitiativeDTO.OperationType.DELETE_INSTRUMENT.name())
                .build();

        // When
        HpanInitiatives result = hpanInitiativeDTO2InitialEntityMapper.apply(hpanInitiativeDTO);

        // Then
        Assertions.assertNotNull(result);
//        TestUtils.checkNullFields(result);
        Assertions.assertNull(result.getUserId());
        Assertions.assertNull(result.getHpan());
        Assertions.assertNull(result.getOnboardedInitiatives());
    }
}