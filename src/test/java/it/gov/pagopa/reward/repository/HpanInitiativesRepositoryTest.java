package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.List;

class HpanInitiativesRepositoryTest extends BaseIntegrationTest {
    @Autowired
    protected HpanInitiativesRepository hpanInitiativesRepository;

    @Test
    void findByUserIdAndInitiative(){
        // Given
        int bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);
        hpanInitiativesRepository.save(hpanInitiatives).block();

        // When
        HpanInitiatives result = hpanInitiativesRepository.findByHpanAndOnboardedInitiativesInitiativeIdIn("HPAN_%d".formatted(bias), List.of("INITIATIVE_%d".formatted(bias))).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("HPAN_%d".formatted(bias),result.getHpan());
        Assertions.assertEquals(1, result.getOnboardedInitiatives().stream().filter(m -> m.getInitiativeId().equals("INITIATIVE_%d".formatted(bias))).count());

    }

    @Test
    void findByUserIdAndInitiativeKO(){
        // Given
        int bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);
        hpanInitiativesRepository.save(hpanInitiatives).block();

        // When
        Mono<HpanInitiatives> result = hpanInitiativesRepository.findByHpanAndOnboardedInitiativesInitiativeIdIn("HPAN_%d".formatted(bias), List.of("INITIATIVE_5"));

        // Then
        Assertions.assertNull(result.block());
    }

}