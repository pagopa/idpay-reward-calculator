package it.gov.pagopa.reward.repository;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {
        "classpath:/mongodbEmbeddedDisabled.properties",
        "classpath:/secrets/mongodbConnectionString.properties"
})
public class TransactionProcessedRepositoryTestIntegrated extends TransactionProcessedRepositoryTest{
}
