package io.github.bluething.java.bolttrack.generator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
final class SnowflakeTrackingNumberGenerator implements TrackingNumberGenerator {
    // bits allocation
    private static final long WORKER_ID_BITS   = 10L;
    private static final long SEQUENCE_BITS    = 12L;
    private static final long MAX_WORKER_ID    = (1L << WORKER_ID_BITS) - 1;
    private static final long MAX_SEQUENCE     = (1L << SEQUENCE_BITS)  - 1;
    private static final long WORKER_SHIFT     = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT  = SEQUENCE_BITS + WORKER_ID_BITS;

    private static final long DEFAULT_EPOCH = Instant.parse("2025-01-01T00:00:00Z")
            .toEpochMilli();

    private final long workerId;

    private long lastTimestamp = -1L;
    private long sequence      =  0L;

    SnowflakeTrackingNumberGenerator(@Value("${snowflake.worker-id}") long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "worker-id must be between 0 and " + MAX_WORKER_ID
            );
        }
        this.workerId = workerId;
    }


    @Override
    public String generateTrackingNumber() {
        long now = System.currentTimeMillis();
        if (now < lastTimestamp) {
            // simple fail-fast on big clock skew
            throw new IllegalStateException(
                    "Clock moved backwards. Refusing to generate id."
            );
        }

        if (now == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // exhausted 4K IDs in one ms → wait for next ms
                while (System.currentTimeMillis() <= lastTimestamp) {
                    Thread.onSpinWait();
                }
                now = System.currentTimeMillis();
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = now;
        long id = ((now - DEFAULT_EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;

        // encode to base-36 uppercase, gives ≤13 chars for 63 bits
        return Long.toString(id, 36).toUpperCase();
    }
}
