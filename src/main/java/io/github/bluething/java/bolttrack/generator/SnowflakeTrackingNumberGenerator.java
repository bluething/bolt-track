package io.github.bluething.java.bolttrack.generator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

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

    /**
     * Packs [lastTimestampRelMs (high bits) | sequence (low bits)].
     * High bits = s >>> SEQUENCE_BITS, low bits = s & MAX_SEQUENCE.
     */
    private final AtomicLong state = new AtomicLong(0L);

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
        while (true) {
            // 1) Snapshot the packed state (timestamp | sequence)
            long previousPackedState = state.get();
            //    high bits = lastTimestampMs, low bits = lastSequenceNumber
            long lastTimestampMs = previousPackedState >>> SEQUENCE_BITS;
            long lastSequenceNumber = previousPackedState & MAX_SEQUENCE;

            // 2) Compute current time relative to our custom epoch
            long currentTimestampMs = System.currentTimeMillis() - DEFAULT_EPOCH;
            if (currentTimestampMs < lastTimestampMs) {
                throw new IllegalStateException(
                        "Clock moved backwards. Refusing to generate ID."
                );
            }

            // 3) Decide next timestamp and sequence
            long nextTimestampMs = lastTimestampMs;
            long nextSequenceNumber;
            if (currentTimestampMs == lastTimestampMs) {
                // same millisecond → bump sequence
                nextSequenceNumber = (lastSequenceNumber + 1) & MAX_SEQUENCE;
                if (nextSequenceNumber == 0) {
                    // sequence overflow: busy‐spin until next ms
                    do {
                        Thread.onSpinWait();
                        currentTimestampMs = System.currentTimeMillis() - DEFAULT_EPOCH;
                    } while (currentTimestampMs <= lastTimestampMs);
                    nextTimestampMs = currentTimestampMs;
                }
            } else {
                // new millisecond → reset sequence
                nextTimestampMs = currentTimestampMs;
                nextSequenceNumber = 0L;
            }

            // 4) Pack next timestamp and sequence
            long nextPackedState =
                    (nextTimestampMs << SEQUENCE_BITS) |
                            nextSequenceNumber;

            // 5) Try to CAS-update; if it succeeds, we own this slot
            if (state.compareAndSet(previousPackedState, nextPackedState)) {
                long rawId =
                        (nextTimestampMs << TIMESTAMP_SHIFT) |
                                (workerId << WORKER_SHIFT) |
                                nextSequenceNumber;
                // 6) Base-36 encode and uppercase → [0-9A-Z]{1,13}
                return Long.toString(rawId, 36).toUpperCase();
            }
            // CAS lost → retry loop
        }
    }
}
