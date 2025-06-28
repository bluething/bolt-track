package io.github.bluething.java.bolttrack.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
class TrackingDetailResponse {
    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("origin_country_id")
    private String originCountryId;

    @JsonProperty("destination_country_id")
    private String destinationCountryId;

    @JsonProperty("weight")
    private BigDecimal weight;

    @JsonProperty("customer_id")
    private UUID customerId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_slug")
    private String customerSlug;

    @JsonProperty("generated_at")
    private Instant generatedAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}
