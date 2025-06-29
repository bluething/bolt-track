package io.github.bluething.java.bolttrack.config;

import io.github.bluething.java.bolttrack.persistence.TrackingNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
class TrackingSystemHealthIndicator implements HealthIndicator {
    private final TrackingNumberRepository repository;
    @Override
    public Health health() {
        try {
            // This issues a findOne with limit(1) under the hood
            Optional<?> any = repository.findAll(PageRequest.of(0,1))
                    .stream().findFirst();
            // we don't care if it's empty—just that it queried successfully
            return Health.up().withDetail("database", "read OK").build();
        } catch (Exception ex) {
            log.error("Db was down {}", ex.getMessage(), ex);
            return Health.down(ex).withDetail("database", "read failed").build();
        }
    }
}
