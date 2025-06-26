package io.github.bluething.java.bolttrack.rest;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record TrackingNumberRequest(
        @NotNull(message = "origin_country_id is required")
        @Pattern(regexp = "^[A-Z]{2}$",
                message = "origin_country_id must be ISO 3166-1 alpha-2 uppercase")
        String origin_country_id,

        @NotNull(message = "destination_country_id is required")
        @Pattern(regexp = "^[A-Z]{2}$",
                message = "destination_country_id must be ISO 3166-1 alpha-2 uppercase")
        String destination_country_id,

        @NotNull(message = "weight is required")
        @Digits(integer = 10, fraction = 3,
                message = "weight must be a number with up to three decimal places")
        BigDecimal weight,

        @NotNull(message = "created_at is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant created_at,

        @NotNull(message = "customer_id is required")
        UUID customer_id,

        @NotBlank(message = "customer_name cannot be blank")
        String customer_name,

        @NotNull(message = "customer_slug is required")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "customer_slug must be kebab-case lowercase")
        String customer_slug
) {
}
