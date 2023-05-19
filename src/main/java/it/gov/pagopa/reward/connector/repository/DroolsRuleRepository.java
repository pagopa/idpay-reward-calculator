package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.model.DroolsRule;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface DroolsRuleRepository extends ReactiveMongoRepository<DroolsRule,String> {
}
