package io.github.bluething.java.bolttrack.domain;

import io.github.bluething.java.bolttrack.exception.InvalidStatusTransitionException;
import io.github.bluething.java.bolttrack.exception.ResourceNotFoundException;
import io.github.bluething.java.bolttrack.persistence.TrackingNumberDocument;
import io.github.bluething.java.bolttrack.persistence.TrackingNumberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class DefaultTrackingNumberService implements TrackingNumberService {
    private final TrackingNumberGenerator generator;
    private final TrackingNumberRepository repository;

    @Override
    public TrackingNumberRecords.TrackingNumberData generate(TrackingNumberRecords.CreateTrackingNumberCommand dto) {
        String trackingNumber = generator.generateTrackingNumber();
        Instant createdAt = Instant.now();

        Instant generatedAt = Instant.now();

        TrackingNumberDocument doc = new TrackingNumberDocument(
                null,
                trackingNumber,
                dto.originCountryId(),
                dto.destinationCountryId(),
                dto.weight(),
                dto.customerId(),
                dto.customerName(),
                dto.customerSlug(),
                createdAt,
                TrackingStatus.CREATED.name(),
                null
        );
        repository.save(doc);

        return new TrackingNumberRecords.TrackingNumberData(
                trackingNumber,
                generatedAt
        );
    }

    @Override
    public TrackingNumberRecords.TrackingDetailData findByTrackingNumber(String trackingNumber) {
        TrackingNumberDocument doc = repository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException("TrackingNumber", trackingNumber)
                );
        return new TrackingNumberRecords.TrackingDetailData(
                doc.getTrackingNumber(),
                doc.getOriginCountryId(),
                doc.getDestinationCountryId(),
                doc.getWeight(),
                doc.getGeneratedAt(),
                doc.getCustomerId(),
                doc.getCustomerName(),
                doc.getCustomerSlug(),
                doc.getGeneratedAt(),
                doc.getStatus(),
                doc.getMetadata()
        );
    }

    @Override
    public TrackingNumberRecords.TrackingDetailData updateStatus(String trackingNumber, String newStatusStr) {
        TrackingNumberDocument doc = repository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException("TrackingNumber", trackingNumber)
                );

        TrackingStatus current = TrackingStatus.valueOf(doc.getStatus());
        TrackingStatus next    = TrackingStatus.valueOf(newStatusStr);

        if (!current.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(current.name(), next.name());
        }

        doc.setStatus(next.name());
        repository.save(doc);

        return new TrackingNumberRecords.TrackingDetailData(
                doc.getTrackingNumber(),
                doc.getOriginCountryId(),
                doc.getDestinationCountryId(),
                doc.getWeight(),
                doc.getGeneratedAt(),
                doc.getCustomerId(),
                doc.getCustomerName(),
                doc.getCustomerSlug(),
                doc.getGeneratedAt(),
                doc.getStatus(),
                doc.getMetadata()
        );
    }
}
