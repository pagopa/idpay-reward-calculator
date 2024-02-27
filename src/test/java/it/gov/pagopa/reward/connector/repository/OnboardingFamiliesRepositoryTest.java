package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import it.gov.pagopa.common.reactive.mongo.config.ReactiveMongoConfig;
import it.gov.pagopa.reward.model.OnboardingFamilies;
import it.gov.pagopa.reward.test.fakers.OnboardingFamiliesFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=4.2.24",
        "spring.data.mongodb.database=idpay",
})
@ExtendWith(SpringExtension.class)
@AutoConfigureSingleInstanceMongodb
@ContextConfiguration(classes = {OnboardingFamiliesRepository.class, ReactiveMongoConfig.class})
class OnboardingFamiliesRepositoryTest {
    @Autowired
    private OnboardingFamiliesRepository repository;

    private final List<OnboardingFamilies> testData = new ArrayList<>();

    @AfterEach
    void clearTestData() {
        repository.deleteAll(testData).block();
    }

    private OnboardingFamilies storeTestFamily(OnboardingFamilies e) {
        OnboardingFamilies out = repository.save(e).block();
        testData.add(out);
        return out;
    }

    @Test
    void testFindByMemberIdsInAndInitiativeId() {
        String initiativeId = "INITIATIVE";
        OnboardingFamilies f1 = OnboardingFamiliesFaker.mockInstance(1);
        f1.setInitiativeId(initiativeId);
        Set<String> membersId1 = Set.of("ID1", "ID2");
        f1.setMemberIds(membersId1);
        storeTestFamily(f1);

        OnboardingFamilies f2 = OnboardingFamiliesFaker.mockInstance(2);
        f2.setInitiativeId(initiativeId);
        Set<String> membersId2 = Set.of("ID2", "ID3");
        f2.setMemberIds(membersId2);
        storeTestFamily(f2);

        //checking if builder custom implementation is successfully handling ids
        assertStoredData();
        Assertions.assertEquals(2, testData.stream().map(OnboardingFamilies::getId).count());

        List<OnboardingFamilies> result = repository.findByMemberIdsInAndInitiativeId("ID1", initiativeId).collectList().block();
        Assertions.assertEquals(List.of(f1), result);

        result = repository.findByMemberIdsInAndInitiativeId("ID2", initiativeId).collectList().block();
        Assertions.assertEquals(List.of(f1, f2), result);

        result = repository.findByMemberIdsInAndInitiativeId("ID4", initiativeId).collectList().block();
        Assertions.assertEquals(Collections.emptyList(), result);
    }

    private void assertStoredData() {
        testData.forEach(t -> Assertions.assertEquals(t, repository.findById(t.getId()).block()));
    }

}