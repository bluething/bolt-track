package io.github.bluething.java.bolttrack.domain;

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
}
