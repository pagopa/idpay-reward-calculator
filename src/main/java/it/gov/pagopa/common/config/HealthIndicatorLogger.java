package it.gov.pagopa.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class HealthIndicatorLogger implements HealthIndicator {

    @Autowired
    private List<HealthIndicator> healthIndicatorList;

    @Override
    public Health health() {
        for (final HealthIndicator current : healthIndicatorList) {
            if (current != this) {
                Health health = current.health();
                if(Status.DOWN.equals(health.getStatus()) || Status.OUT_OF_SERVICE.equals(health.getStatus())){
                    log.info("[HEALTH][{}] {}: {}", health.getStatus(), current.getClass().getSimpleName(), health.getDetails());
                }
            }
        }
        return Health.up().build();
    }
}