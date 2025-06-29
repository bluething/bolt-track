package io.github.bluething.java.bolttrack.domain;

import io.github.bluething.java.bolttrack.exception.InvalidStatusTransitionException;
import io.github.bluething.java.bolttrack.exception.ResourceNotFoundException;
import io.github.bluething.java.bolttrack.persistence.TrackingNumberDocument;
import io.github.bluething.java.bolttrack.persistence.TrackingNumberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultTrackingNumberServiceTest {
    @Mock
    TrackingNumberGenerator generator;

    @Mock
    TrackingNumberRepository repository;

    @InjectMocks
    DefaultTrackingNumberService service;

    private TrackingNumberRecords.CreateTrackingNumberCommand cmd;
    private final UUID customerId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    @BeforeEach
    void setUp() {
        cmd = new TrackingNumberRecords.CreateTrackingNumberCommand(
                "US", "CA",
                new BigDecimal("2.500"),
                Instant.parse("2025-06-26T08:00:00Z"),
                customerId,
                "Acme Corp",
                "acme-corp"
        );
    }

    @Test
    @DisplayName("generate(...) should call generator and repository.save, and return correct DTO")
    void generate_callsGeneratorAndSave() {
        when(generator.generateTrackingNumber()).thenReturn("TRACK123");
        TrackingNumberRecords.TrackingNumberData result = service.generate(cmd);

        assertThat(result.trackingNumber()).isEqualTo("TRACK123");
        assertThat(result.createdAt()).isNotNull();

        // Capture the document saved
        @SuppressWarnings("unchecked")
        ArgumentCaptor<TrackingNumberDocument> captor =
                ArgumentCaptor.forClass(TrackingNumberDocument.class);
        verify(repository).save(captor.capture());

        TrackingNumberDocument saved = captor.getValue();
        assertThat(saved.getTrackingNumber()).isEqualTo("TRACK123");
        assertThat(saved.getOriginCountryId()).isEqualTo("US");
        assertThat(saved.getDestinationCountryId()).isEqualTo("CA");
        assertThat(saved.getWeight()).isEqualByComparingTo(new BigDecimal("2.500"));
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getCustomerName()).isEqualTo("Acme Corp");
        assertThat(saved.getCustomerSlug()).isEqualTo("acme-corp");
        assertThat(saved.getStatus()).isEqualTo(TrackingStatus.CREATED.name());
        // metadata should be null on creation
        assertThat(saved.getMetadata()).isNull();
    }

    @Test
    @DisplayName("findByTrackingNumber(...) returns correct detail DTO when repository has a document")
    void findByTrackingNumber_found_mapsToDetail() {
        TrackingNumberDocument doc = new TrackingNumberDocument(
                "some-id",
                "TRACK123",
                "US", "CA",
                new BigDecimal("2.500"),
                customerId,
                "Acme Corp",
                "acme-corp",
                Instant.parse("2025-06-26T08:00:00Z"),
                "CREATED",
                null
        );
        // Note: TrackingDetailData expects generatedAt same as document.getGeneratedAt()
        doc.setGeneratedAt(Instant.parse("2025-06-26T08:01:00Z"));
        when(repository.findByTrackingNumber("TRACK123"))
                .thenReturn(Optional.of(doc));

        TrackingNumberRecords.TrackingDetailData detail =
                service.findByTrackingNumber("TRACK123");

        assertThat(detail.trackingNumber()).isEqualTo("TRACK123");
        assertThat(detail.originCountryId()).isEqualTo("US");
        assertThat(detail.destinationCountryId()).isEqualTo("CA");
        assertThat(detail.weight()).isEqualByComparingTo(new BigDecimal("2.500"));
        assertThat(detail.generatedAt())
                .isEqualTo(Instant.parse("2025-06-26T08:01:00Z"));
        assertThat(detail.customerId()).isEqualTo(customerId);
        assertThat(detail.customerName()).isEqualTo("Acme Corp");
        assertThat(detail.customerSlug()).isEqualTo("acme-corp");
        assertThat(detail.status()).isEqualTo("CREATED");
        assertThat(detail.metadata()).isNull();
    }

    @Test
    @DisplayName("findByTrackingNumber(...) throws ResourceNotFoundException when absent")
    void findByTrackingNumber_notFound_throws() {
        when(repository.findByTrackingNumber("MISSING"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.findByTrackingNumber("MISSING"));
    }

    @Test
    @DisplayName("updateStatus(...) successfully updates status on valid transition")
    void updateStatus_validTransition_savesAndReturns() {
        // existing doc in CREATED
        TrackingNumberDocument doc = new TrackingNumberDocument(
                "id", "TRACK123", "US", "CA",
                new BigDecimal("2.0"), customerId,
                "Acme Corp", "acme-corp",
                Instant.parse("2025-06-26T08:00:00Z"),
                "CREATED",
                null
        );
        doc.setGeneratedAt(Instant.parse("2025-06-26T08:01:00Z"));
        when(repository.findByTrackingNumber("TRACK123"))
                .thenReturn(Optional.of(doc));

        TrackingNumberRecords.TrackingDetailData updated =
                service.updateStatus("TRACK123", "PICKED_UP");

        // Assert DTO
        assertThat(updated.status()).isEqualTo("PICKED_UP");

        // Verify save called with updated doc
        assertThat(doc.getStatus()).isEqualTo("PICKED_UP");
        verify(repository).save(doc);
    }

    @Test
    @DisplayName("updateStatus(...) throws InvalidStatusTransitionException on invalid jump")
    void updateStatus_invalidTransition_throws() {
        // existing doc in CREATED
        TrackingNumberDocument doc = new TrackingNumberDocument();
        doc.setTrackingNumber("TRACK123");
        doc.setStatus("CREATED");
        when(repository.findByTrackingNumber("TRACK123"))
                .thenReturn(Optional.of(doc));

        assertThatThrownBy(() ->
                service.updateStatus("TRACK123", "DELIVERED")
        ).isInstanceOf(InvalidStatusTransitionException.class);
        // repository.save should not be called
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus(...) throws ResourceNotFoundException when absent")
    void updateStatus_notFound_throws() {
        when(repository.findByTrackingNumber("NOPE"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateStatus("NOPE", "PICKED_UP"));
    }

}