package io.github.bluething.java.bolttrack.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Document(collection = "tracking_numbers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingNumberDocument {
    @Id
    private String id;

    @Field("tracking_number")
    @Indexed(unique = true, background = true)
    private String trackingNumber;

    @Field("origin_country_id")
    private String originCountryId;

    @Field("destination_country_id")
    private String destinationCountryId;

    @Field(value = "weight", targetType = FieldType.DECIMAL128)
    private BigDecimal weight;

    @Field("customer_id")
    private UUID customerId;

    @Field("customer_name")
    private String customerName;

    @Field("customer_slug")
    private String customerSlug;

    @CreatedDate
    @Field("generated_at")
    @Indexed(name = "generatedAt_ttl_idx", expireAfter = "P1Y")
    private Instant generatedAt;

    @Field("status")
    private String status;

    @Field("metadata")
    private Map<String, Object> metadata;
}
