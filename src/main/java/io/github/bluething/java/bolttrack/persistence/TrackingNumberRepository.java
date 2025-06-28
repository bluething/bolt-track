package io.github.bluething.java.bolttrack.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackingNumberRepository extends MongoRepository<TrackingNumberDocument, String> {
    /**
     * Find a tracking record by its generated tracking number.
     * @param trackingNumber the unique tracking number
     * @return optional TrackingNumberDocument
     */
    Optional<TrackingNumberDocument> findByTrackingNumber(String trackingNumber);
}
