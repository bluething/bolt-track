package io.github.bluething.java.bolttrack.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
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

    public record TrackingDetailData(
            String trackingNumber,
            String originCountryId,
            String destinationCountryId,
            BigDecimal weight,
            Instant orderCreatedAt,
            UUID customerId,
            String customerName,
            String customerSlug,
            Instant generatedAt,
            String status,
            Map<String, Object> metadata
    ) {
    }
}
