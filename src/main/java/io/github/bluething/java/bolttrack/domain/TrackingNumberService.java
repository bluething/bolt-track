package io.github.bluething.java.bolttrack.domain;

public interface TrackingNumberService {
    TrackingNumberRecords.TrackingNumberData generate(TrackingNumberRecords.CreateTrackingNumberCommand dto);
    TrackingNumberRecords.TrackingDetailData findByTrackingNumber(String trackingNumber);
    TrackingNumberRecords.TrackingDetailData updateStatus(String trackingNumber, String newStatusStr);
}
