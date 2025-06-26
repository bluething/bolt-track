package io.github.bluething.java.bolttrack.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
class TrackingNumberResponse {
    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("created_at")
    private Instant createdAt;
}
