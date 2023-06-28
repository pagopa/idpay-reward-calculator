package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HpanInitiativesRepositoryTest extends BaseIntegrationTest {
    private static final String USERID = "USERID";
    private static final String INITIATIVEID = "INITIATIVEID";
    private static final String HPAN_TEST = "HPAN_TEST_%d";
    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;

    @BeforeEach
    void setUp() {
        LocalDateTime onboardedTime = LocalDateTime.now().with(LocalTime.MIN);
        ActiveTimeInterval interval = ActiveTimeInterval.builder().startInterval(onboardedTime.minusMonths(5L).plusDays(1L)).build();

        List<HpanInitiatives> hpanInitiativesList = IntStream.range(0, 3).mapToObj(i -> {
                    String status = switch (i % 3) {
                        case 0 -> HpanInitiativeConstants.STATUS_ACTIVE;
                        case 1 -> HpanInitiativeConstants.STATUS_UPDATE;
                        default -> HpanInitiativeConstants.STATUS_INACTIVE;
                    };
                    OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                            .initiativeId(String.format(INITIATIVEID))
                            .status(status)
                            .activeTimeIntervals(new ArrayList<>()).build();

                    onboardedInitiative.getActiveTimeIntervals().add(interval);

                    return Pair.of(i, onboardedInitiative);
                })
                .map(pairHpan -> HpanInitiatives.builder()
                        .hpan(HPAN_TEST.formatted(pairHpan.getFirst()))
                        .userId(USERID)
                        .onboardedInitiatives(List.of(pairHpan.getSecond()))
                        .build())
                .toList();


        hpanInitiativesRepository.saveAll(hpanInitiativesList).collectList().block();
    }

    @AfterEach
    void clearData() {
        List<String> hpans = IntStream.range(0, 3).mapToObj(HPAN_TEST::formatted).toList();
        hpanInitiativesRepository.deleteAllById(hpans).block();
    }

    @Test
    void retrieveAHpanByUserIdAndInitiativeIdAndStatus(){
        List<HpanInitiatives> result = hpanInitiativesRepository.retrieveAHpanByUserIdAndInitiativeIdAndStatus(USERID,
                INITIATIVEID,
                HpanInitiativeConstants.STATUS_ACTIVE, HpanInitiativeConstants.STATUS_UPDATE)
                .collectList().block();

        assertNotNull(result);
        assertEquals(2, result.size());
    }
    @Test
    void retrieveHpanByUserIdAndInitiativeIdAndStatusInactive() {
        List<HpanInitiatives> result = hpanInitiativesRepository.retrieveAHpanByUserIdAndInitiativeIdAndStatus(USERID, INITIATIVEID, HpanInitiativeConstants.STATUS_INACTIVE).collectList().block();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}