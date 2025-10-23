package it.gov.pagopa.common.reactive.mongo.config;

import it.gov.pagopa.common.reactive.mongo.ReactiveMongoRepositoryImpl;
import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.OnboardingFamiliesRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@Configuration
@EnableReactiveMongoRepositories(
        basePackages = "it.gov.pagopa",
        repositoryBaseClass = ReactiveMongoRepositoryImpl.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {DroolsRuleRepository.class, HpanInitiativesRepository.class, OnboardingFamiliesRepository.class})
)
public class ReactiveMongoConfig {
}
