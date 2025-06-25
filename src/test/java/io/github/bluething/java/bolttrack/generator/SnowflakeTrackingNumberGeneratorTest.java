package io.github.bluething.java.bolttrack.generator;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

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

    @Test
    void generateConcurrentlyWithVirtualThreads() throws InterruptedException {
        var gen = new SnowflakeTrackingNumberGenerator(3);
        int virtualThreads = 5_000;      // spin up 5k virtual threads
        int perThread     = 100;        // each generates 100 IDs
        Set<String> trackingNumbers   = ConcurrentHashMap.newKeySet();

        // Create an ExecutorService that uses one virtual thread per task
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Submit one task per virtual thread
            var tasks = IntStream.range(0, virtualThreads)
                    .<Callable<Void>>mapToObj(i -> () -> {
                        for (int j = 0; j < perThread; j++) {
                            trackingNumbers.add(gen.generateTrackingNumber());
                        }
                        return null;
                    })
                    .toList();

            // Wait for trackingNumbers to complete
            executor.invokeAll(tasks);
        }

        int expected = virtualThreads * perThread;
        assertEquals(expected, trackingNumbers.size(),
                "Expected trackingNumbers IDs across virtual threads to be unique");
    }

}