package io.github.bluething.java.bolttrack.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeTrackingNumberGeneratorTest {

    @Test
    void constructorRejectsNegativeWorkerId() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new SnowflakeTrackingNumberGenerator(-1)
        );
        assertTrue(ex.getMessage().contains("worker-id"));
    }

    @Test
    void constructorRejectsTooLargeWorkerId() {
        // MAX_WORKER_ID = (1<<10) - 1 = 1023
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new SnowflakeTrackingNumberGenerator(1024)
        );
        assertTrue(ex.getMessage().contains("worker-id"));
    }

}