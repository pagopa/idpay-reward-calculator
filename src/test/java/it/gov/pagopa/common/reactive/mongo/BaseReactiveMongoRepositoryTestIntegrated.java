package it.gov.pagopa.common.reactive.mongo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
class BaseReactiveMongoRepositoryTestIntegrated extends BaseReactiveMongoRepositoryIntegrationTest {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Override
    @Test
    void testFindById() {
        super.testFindById();

        org.bson.Document result = mongoTemplate.executeCommand(new org.bson.Document("getLastRequestStatistics", 1)).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("find", result.get("CommandName"));
        double ru = (double) result.get("RequestCharge");
        Assertions.assertTrue(ru <= 4.0, "Unexpected RU consumed! " + ru);
    }
}
