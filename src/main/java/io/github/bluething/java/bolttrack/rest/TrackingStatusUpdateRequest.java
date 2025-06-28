package io.github.bluething.java.bolttrack.rest;

import jakarta.validation.constraints.NotNull;

record TrackingStatusUpdateRequest(
        @NotNull(message = "status is required")
        String status
) {
}
