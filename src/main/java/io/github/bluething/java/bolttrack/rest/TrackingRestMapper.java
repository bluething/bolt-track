package io.github.bluething.java.bolttrack.rest;

import io.github.bluething.java.bolttrack.domain.TrackingNumberRecords;

class TrackingRestMapper {
    public static TrackingNumberResponse toRest(TrackingNumberRecords.TrackingNumberData dto) {
        return new TrackingNumberResponse(
                dto.trackingNumber(),
                dto.createdAt()
        );
    }
    public static TrackingNumberRecords.CreateTrackingNumberCommand toDto(TrackingNumberRequest rest) {
        return new TrackingNumberRecords.CreateTrackingNumberCommand(rest.origin_country_id(),
                rest.destination_country_id(),
                rest.weight(),
                rest.created_at(),
                rest.customer_id(),
                rest.customer_name(),
                rest.customer_slug());
    }
    public static TrackingDetailResponse toDetailRest(TrackingNumberRecords.TrackingDetailData dto) {
        return new TrackingDetailResponse(dto.trackingNumber(),
                dto.originCountryId(),
                dto.destinationCountryId(),
                dto.weight(),
                dto.customerId(),
                dto.customerName(),
                dto.customerSlug(),
                dto.generatedAt(),
                dto.status(),
                dto.metadata());
    }
}
