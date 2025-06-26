package io.github.bluething.java.bolttrack.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TrackingNumberRecords {
    public record TrackingNumberData(
            String trackingNumber,
            Instant createdAt
    ) {
    }

    public record CreateTrackingNumberCommand(String originCountryId,
                                              String destinationCountryId,
                                              BigDecimal weight,
                                              Instant createdAt,
                                              UUID customerId,
                                              String customerName,
                                              String customerSlug) {
    }
}
