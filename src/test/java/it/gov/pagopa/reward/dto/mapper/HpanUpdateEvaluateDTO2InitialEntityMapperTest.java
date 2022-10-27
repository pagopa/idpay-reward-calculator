package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class HpanUpdateEvaluateDTO2InitialEntityMapperTest {

    @Test
    void addedApply() {
        // Given
        HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper = new HpanUpdateEvaluateDTO2HpanInitiativeMapper();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = HpanInitiativeDTOFaker.mockInstanceBuilder(1)
                .evaluationDate(LocalDateTime.now())
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .build();

        // When
        HpanInitiatives result = hpanUpdateEvaluateDTO2HpanInitiativeMapper.apply(hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "onboardedInitiatives");
    }

    @Test
    void deletedApply() {
        // Given
        HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper = new HpanUpdateEvaluateDTO2HpanInitiativeMapper();

        HpanUpdateEvaluateDTO hpanUpdateEvaluateDTO = HpanInitiativeDTOFaker.mockInstanceBuilder(1)
                .evaluationDate(LocalDateTime.now())
                .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
                .build();

        // When
        HpanInitiatives result = hpanUpdateEvaluateDTO2HpanInitiativeMapper.apply(hpanUpdateEvaluateDTO);

        // Then
        Assertions.assertNull(result);
    }
}