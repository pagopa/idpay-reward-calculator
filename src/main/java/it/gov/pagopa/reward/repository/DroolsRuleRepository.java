package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.DroolsRule;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface DroolsRuleRepository extends ReactiveMongoRepository<DroolsRule,String> {
}
