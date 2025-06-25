package io.github.bluething.java.bolttrack.generator;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeTrackingNumberGeneratorTest {
    // matches 1–16 uppercase alphanumeric chars
    private static final Pattern TN_PATTERN = Pattern.compile("^[0-9A-Z]{1,16}$");

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

    @Test
    void generateProducesValidFormatAndLength() {
        var gen = new SnowflakeTrackingNumberGenerator(5);
        String tn = gen.generateTrackingNumber();

        assertNotNull(tn);
        assertTrue(TN_PATTERN.matcher(tn).matches(),
                () -> "Generated tracking number has invalid format: " + tn);
        assertTrue(tn.length() <= 16,
                () -> "Tracking number too long: " + tn.length());
    }

}