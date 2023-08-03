package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

class HpanInitiativesAtomicOpsRepositoryImplTest extends BaseIntegrationTest {
    @Autowired
    protected HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    private HpanInitiativesAtomicOpsRepositoryImpl hpanInitiativesAtomicOpsRepositoryImpl;

    @AfterEach
    void clearData() {
        hpanInitiativesRepository.deleteAllById(
                List.of("hpan_prova", "hpan_prova_1")).block();
    }

    @Test
    void createIfNotExist() {
        String hpan = "hpan_prova";
        HpanInitiatives hpanInitiativesFirst = HpanInitiatives.builder()
                .userId("USERID")
                .hpan(hpan)
                .maskedPan("MASKED_PAN")
                .brandLogo("BRAND_LOGO")
                .brand("BRAND").build();

        List<OnboardedInitiative> onboardedInitiativeList = new ArrayList<>();

        HpanInitiatives hpanInitiativesSecond = HpanInitiatives.builder()
                .hpan(hpan)
                .onboardedInitiatives(onboardedInitiativeList).build();

        hpanInitiativesAtomicOpsRepositoryImpl.createIfNotExist(hpanInitiativesFirst).block();
        HpanInitiatives afterFirstSave = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(afterFirstSave);
        TestUtils.checkNotNullFields(afterFirstSave, "onboardedInitiatives");

        hpanInitiativesAtomicOpsRepositoryImpl.createIfNotExist(hpanInitiativesSecond).block();
        HpanInitiatives result = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "onboardedInitiatives");
        Assertions.assertEquals(hpanInitiativesFirst.getUserId(), result.getUserId());
        Assertions.assertEquals(hpan, result.getHpan());
    }

    @Test
    void setInitiative() {
        String hpan = "hpan_prova";

        LocalDateTime now = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = now.minusDays(2L).with(LocalTime.MAX);

        List<ActiveTimeInterval> activeIntervalList = getActiveTimeIntervalList(now, end);

        storeHpanInitiatives(hpan, now, end, activeIntervalList);

        ActiveTimeInterval activeTimeInterval2 = ActiveTimeInterval.builder()
                .startInterval(now).build();
        activeIntervalList.add(activeTimeInterval2);

        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        final List<Future<UpdateResult>> tasks = IntStream.range(1, 3)
                .mapToObj(i -> OnboardedInitiative.builder()
                        .initiativeId("INITIATIVE_%d".formatted(i))
                        .acceptanceDate(now.minusDays(10L))
                        .updateDate(end)
                        .status(HpanInitiativeStatus.ACTIVE)
                        .activeTimeIntervals(activeIntervalList).build()
                )
                .map(oi -> executorService.submit(() -> hpanInitiativesAtomicOpsRepositoryImpl.setInitiative(hpan, oi).block())).toList();

        tasks.forEach(updateResultFuture -> {
            try {
                long modifiedCount = updateResultFuture.get().getModifiedCount();
                Assertions.assertEquals(1, modifiedCount);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        HpanInitiatives result = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(result);
        result.getOnboardedInitiatives().forEach(onboardedInitiative ->
                Assertions.assertEquals(2, onboardedInitiative.getActiveTimeIntervals().size()));

        //set another initiative
        List<ActiveTimeInterval> activeTimeIntervalList2 = List.of(ActiveTimeInterval.builder().startInterval(now).build());
        setAnotherInitiative(hpan, activeTimeIntervalList2);

        HpanInitiatives result2 = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(result2);
        Assertions.assertEquals(3, result2.getOnboardedInitiatives().size());
        OnboardedInitiative onboardedInitiativeResult2 = result2.getOnboardedInitiatives().stream().filter(o -> o.getInitiativeId().equals("ANOTHER_INITIATIVE")).findFirst().orElse(null);
        Assertions.assertNotNull(onboardedInitiativeResult2);
        Assertions.assertEquals(1, onboardedInitiativeResult2.getActiveTimeIntervals().size());
        Assertions.assertEquals(activeTimeIntervalList2, onboardedInitiativeResult2.getActiveTimeIntervals());

        // testing removing initiatives due not active intervals
        removeInitiative("ANOTHER_INITIATIVE", hpan, List.of("INITIATIVE_1", "INITIATIVE_2"));
        removeInitiative("INITIATIVE_2", hpan, List.of("INITIATIVE_1"));
        removeInitiative("INITIATIVE_1", hpan, Collections.emptyList());
    }

    private void removeInitiative(String initiative2remove, String hpan, List<String> expectedInitiatives) {
        OnboardedInitiative removedInitiative2 = OnboardedInitiative.builder()
                .initiativeId(initiative2remove)
                .activeTimeIntervals(Collections.emptyList())
                .build();
        hpanInitiativesAtomicOpsRepositoryImpl.setInitiative(hpan, removedInitiative2).block();

        HpanInitiatives result = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedInitiatives, result.getOnboardedInitiatives().stream().map(OnboardedInitiative::getInitiativeId).toList());
    }

    @Test
    void createIfNotExistConcurrency() {
        String hpan = "hpan_prova";
        String maskedPan = "MASKEDPAN_CONCURRENCY";
        String brandLogo = "BRANDLOGO_CONCURRENCY";
        String brand = "BRAND_CONCURRENCY";
        String userId = "USERID_CONCURRENCY";

        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        final List<Future<UpdateResult>> tasks = IntStream.range(0, 2)
                .mapToObj(i -> executorService.submit(() -> {
                    HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                            .hpan(hpan)
                            .maskedPan(maskedPan)
                            .brandLogo(brandLogo)
                            .brand(brand)
                            .userId(userId)
                            .build();
                    return hpanInitiativesAtomicOpsRepositoryImpl.createIfNotExist(hpanInitiatives).block();
                })).toList();

        tasks.forEach(updateResultFuture -> {
            try {
                long modifiedCount = updateResultFuture.get().getModifiedCount();
                Assertions.assertEquals(0, modifiedCount);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        HpanInitiatives result = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "onboardedInitiatives");
        Assertions.assertEquals(userId, result.getUserId());
        Assertions.assertEquals(hpan, result.getHpan());
    }

    private void setAnotherInitiative(String hpan, List<ActiveTimeInterval> activeTimeIntervalList) {
        OnboardedInitiative onboardedInitiative2 = OnboardedInitiative.builder().initiativeId("ANOTHER_INITIATIVE")
                .activeTimeIntervals(activeTimeIntervalList)
                .build();
        hpanInitiativesAtomicOpsRepositoryImpl.setInitiative(hpan, onboardedInitiative2).block();
    }

    private List<ActiveTimeInterval> getActiveTimeIntervalList(LocalDateTime now, LocalDateTime end) {
        List<ActiveTimeInterval> activeIntervalList = new ArrayList<>();
        ActiveTimeInterval activeInterval1 = ActiveTimeInterval.builder().startInterval(now.minusDays(10L)).endInterval(end).build();
        activeIntervalList.add(activeInterval1);
        return activeIntervalList;
    }

    private void storeHpanInitiatives(String hpan, LocalDateTime now, LocalDateTime end, List<ActiveTimeInterval> activeIntervalList) {
        List<OnboardedInitiative> onboardedInitiativeList = new ArrayList<>();
        OnboardedInitiative onboardedInitiativeBase1 = OnboardedInitiative.builder()
                .initiativeId("INITIATIVE_1")
                .acceptanceDate(now.minusDays(10L))
                .updateDate(end)
                .lastEndInterval(end)
                .status(HpanInitiativeStatus.ACTIVE)
                .activeTimeIntervals(activeIntervalList).build();
        OnboardedInitiative onboardedInitiativeBase2 = OnboardedInitiative.builder()
                .initiativeId("INITIATIVE_2")
                .acceptanceDate(now.minusDays(10L))
                .updateDate(end)
                .lastEndInterval(end)
                .status(HpanInitiativeStatus.ACTIVE)
                .activeTimeIntervals(activeIntervalList).build();

        onboardedInitiativeList.add(onboardedInitiativeBase1);
        onboardedInitiativeList.add(onboardedInitiativeBase2);


        HpanInitiatives hpanInitiatives = HpanInitiatives.builder()
                .userId("USERID")
                .hpan(hpan)
                .maskedPan("MASKEDPAN")
                .brandLogo("BRANDLOGO")
                .brand("BRAND")
                .onboardedInitiatives(onboardedInitiativeList).build();

        hpanInitiativesRepository.save(hpanInitiatives).block();
    }

    @Test
    void setStatus(){
        String hpan = "hpan_prova";
        String hpan1 = "hpan_prova_1";

        LocalDateTime now = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = now.minusDays(2L).with(LocalTime.MAX);

        List<ActiveTimeInterval> activeIntervalList = getActiveTimeIntervalList(now, end);

        storeHpanInitiatives(hpan, now, end, activeIntervalList);
        storeHpanInitiatives(hpan1, now, end, activeIntervalList);

        UpdateResult result = hpanInitiativesRepository.setUserInitiativeStatus("USERID", "INITIATIVE_2", HpanInitiativeStatus.INACTIVE).block();
        Assertions.assertNotNull(result);

        HpanInitiatives hpanAfterInactiveCall = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(hpanAfterInactiveCall);
        OnboardedInitiative initiative = hpanAfterInactiveCall.getOnboardedInitiatives().get(1);
        Assertions.assertEquals(HpanInitiativeStatus.INACTIVE, initiative.getStatus());
        Assertions.assertFalse(initiative.getUpdateDate().isBefore(end));

        HpanInitiatives hpanAfterInactiveCall1 = hpanInitiativesRepository.findById(hpan1).block();
        Assertions.assertNotNull(hpanAfterInactiveCall1);
        OnboardedInitiative initiative1 = hpanAfterInactiveCall1.getOnboardedInitiatives().get(1);
        Assertions.assertEquals(HpanInitiativeStatus.INACTIVE, initiative1.getStatus());
        Assertions.assertFalse(initiative.getUpdateDate().isBefore(end));

        // reactivate
        UpdateResult resultReactivate = hpanInitiativesRepository.setUserInitiativeStatus("USERID", "INITIATIVE_2", HpanInitiativeStatus.ACTIVE).block();
        Assertions.assertNotNull(resultReactivate);
        HpanInitiatives hpanAfterActiveCall = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(hpanAfterActiveCall);
        OnboardedInitiative initiativeActive = hpanAfterActiveCall.getOnboardedInitiatives().get(1);
        Assertions.assertEquals(HpanInitiativeStatus.ACTIVE, initiativeActive.getStatus());

        HpanInitiatives hpanAfterActiveCall1 = hpanInitiativesRepository.findById(hpan).block();
        Assertions.assertNotNull(hpanAfterActiveCall1);
        OnboardedInitiative initiativeActivate1 = hpanAfterActiveCall1.getOnboardedInitiatives().get(1);
        Assertions.assertEquals(HpanInitiativeStatus.ACTIVE, initiativeActivate1.getStatus());
    }

    @Test
    void findAndRemoveInitiativeOnHpan(){
        LocalDateTime now = LocalDateTime.now();
        OnboardedInitiative onboardings1 = OnboardedInitiative.builder()
                .initiativeId("INITIATIVEID_1")
                .familyId("FAM")
                .activeTimeIntervals(List.of(ActiveTimeInterval.builder()
                        .startInterval(now.minusMonths(2L))
                        .endInterval(now.minusDays(2L))
                        .build()))
                .build();
        OnboardedInitiative onboardings2 = OnboardedInitiative.builder()
                .initiativeId("INITIATIVEID_2")
                .activeTimeIntervals(List.of(ActiveTimeInterval.builder()
                        .startInterval(now.minusMonths(1L))
                        .build()))
                .build();
        HpanInitiatives hpanInitiatives1 = HpanInitiativesFaker.mockInstance(1);
        hpanInitiatives1.setHpan("hpan_prova");
        hpanInitiatives1.setOnboardedInitiatives(List.of(onboardings1, onboardings2));

        HpanInitiatives hpanInitiatives2 = HpanInitiativesFaker.mockInstance(2);
        hpanInitiatives2.setHpan("hpan_prova_1");
        hpanInitiatives2.setOnboardedInitiatives(List.of(onboardings1));

        hpanInitiativesRepository.saveAll(List.of(hpanInitiatives1, hpanInitiatives2)).blockLast();

        hpanInitiativesRepository.findAndRemoveInitiativeOnHpan("INITIATIVEID_1").block();

        Long countElements = hpanInitiativesRepository.findAll()
                .filter(hpanInitiatives -> hpanInitiatives.getOnboardedInitiatives().stream().anyMatch(o -> "INITIATIVE_1".equals(o.getInitiativeId()))).count().block();
        Assertions.assertEquals(0, countElements);

    }

}

