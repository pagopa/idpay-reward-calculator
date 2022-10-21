package it.gov.pagopa.reward.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.reward-rule.rule-engine")
@Data
public class RuleEngineConfig {
    private boolean shortCircuitConditions;
}
