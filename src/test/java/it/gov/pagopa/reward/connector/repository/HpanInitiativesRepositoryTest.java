package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HpanInitiativesRepositoryTest extends BaseIntegrationTest {
    private static final String USERID = "USERID";
    private static final String INITIATIVEID = "INITIATIVEID";
    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;

    @BeforeEach
    void setUp() {
        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(String.format(INITIATIVEID))
                .status("ACCEPTED")
                .activeTimeIntervals(new ArrayList<>()).build();

        LocalDateTime onboardedTime = LocalDateTime.now().with(LocalTime.MIN);
        ActiveTimeInterval interval = ActiveTimeInterval.builder().startInterval(onboardedTime.minusMonths(5L).plusDays(1L)).build();
        onboardedInitiative.getActiveTimeIntervals().add(interval);

        HpanInitiatives hpanInitiative1 = HpanInitiativesFaker.mockInstanceWithoutInitiative(1);
        hpanInitiative1.setHpan("HPAN_TEST_1");
        hpanInitiative1.setUserId(USERID);
        hpanInitiative1.setOnboardedInitiatives(List.of(onboardedInitiative));

        HpanInitiatives hpanInitiative2 = HpanInitiativesFaker.mockInstanceWithoutInitiative(2);
        hpanInitiative2.setHpan("HPAN_TEST_2");
        hpanInitiative2.setUserId(USERID);
        hpanInitiative2.setOnboardedInitiatives(List.of(onboardedInitiative));

        hpanInitiativesRepository.saveAll(List.of(hpanInitiative1, hpanInitiative2)).collectList().block();
    }

    @AfterEach
    void clearData() {
        hpanInitiativesRepository.deleteAllById(List.of("HPAN_TEST_1", "HPAN_TEST_2")).block();
    }

    @Test
    void retrieveHpanByUserIdAndInitiativeId() {
        List<HpanInitiatives> result = hpanInitiativesRepository.retrieveHpanByUserIdAndInitiativeId(USERID, INITIATIVEID).collectList().block();

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}