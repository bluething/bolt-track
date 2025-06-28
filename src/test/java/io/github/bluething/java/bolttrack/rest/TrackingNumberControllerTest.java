package io.github.bluething.java.bolttrack.rest;

import io.github.bluething.java.bolttrack.domain.TrackingNumberRecords;
import io.github.bluething.java.bolttrack.domain.TrackingNumberService;
import io.github.bluething.java.bolttrack.exception.GlobalExceptionHandler;
import io.github.bluething.java.bolttrack.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = TrackingNumberController.class)
@Import({GlobalExceptionHandler.class})
class TrackingNumberControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackingNumberService service;

    @Test
    @DisplayName("GET /next-tracking-number with valid params returns 200 + JSON")
    void nextViaGet_validRequest_success() throws Exception {
        // Arrange
        Instant now = Instant.parse("2025-06-26T10:00:00Z");
        var dto = new TrackingNumberRecords.TrackingNumberData("ABC123XYZ", now);
        when(service.generate(any(TrackingNumberRecords.CreateTrackingNumberCommand.class)))
                .thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/next-tracking-number")
                        .param("origin_country_id",      "MY")
                        .param("destination_country_id", "ID")
                        .param("weight",                 "1.234")
                        .param("created_at",             "2025-06-26T10:00:00+00:00")
                        .param("customer_id",            "de619854-b59b-425e-9db4-943979e1bd49")
                        .param("customer_name",          "RedBox Logistics")
                        .param("customer_slug",          "redbox-logistics")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tracking_number").value("ABC123XYZ"))
                .andExpect(jsonPath("$.created_at").value(now.toString()));

        // verify service was called with properly mapped command
        ArgumentCaptor<TrackingNumberRecords.CreateTrackingNumberCommand> captor =
                ArgumentCaptor.forClass(TrackingNumberRecords.CreateTrackingNumberCommand.class);
        verify(service).generate(captor.capture());
        var cmd = captor.getValue();
        assert cmd.originCountryId().equals("MY");
        assert cmd.destinationCountryId().equals("ID");
        assert cmd.weight().equals(new BigDecimal("1.234"));
        assert cmd.createdAt().equals(Instant.parse("2025-06-26T10:00:00Z"));
        assert cmd.customerId().equals(UUID.fromString("de619854-b59b-425e-9db4-943979e1bd49"));
        assert cmd.customerName().equals("RedBox Logistics");
        assert cmd.customerSlug().equals("redbox-logistics");
    }

    @Test
    @DisplayName("GET /next-tracking-number with invalid params returns 400")
    void nextViaGet_invalidRequest_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/next-tracking-number")
                        // origin_country_id must be 2 uppercase letters
                        .param("origin_country_id", "USA")
                        .param("destination_country_id", "ID")
                        .param("weight", "1.234")
                        .param("created_at", "not-a-timestamp")
                        .param("customer_id", "not-a-uuid")
                        .param("customer_name", "")
                        .param("customer_slug", "Invalid Slug")  // not kebab-case
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());

        // service.generate(...) should never be called on validation failure
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET /next-tracking-number with invalid params returns ApiError JSON")
    void nextViaGet_invalidRequest_returnsApiErrorBody() throws Exception {
        mockMvc.perform(get("/api/v1/next-tracking-number")
                        // invalid origin_country_id (should be exactly 2 uppercase letters)
                        .param("origin_country_id", "USA")
                        .param("destination_country_id", "ID")
                        .param("weight", "1.234")
                        .param("created_at", "2025-06-26T10:00:00+00:00")
                        .param("customer_id", "de619854-b59b-425e-9db4-943979e1bd49")
                        .param("customer_name", "RedBox Logistics")
                        .param("customer_slug", "redbox-logistics")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("origin_country_id"))
                .andExpect(jsonPath("$.path").value("/api/v1/next-tracking-number"));

        // service.generate(...) should never be called on validation failure
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET /api/v1/track/{tracking_number} – existing → 200 + detail JSON")
    void detail_existingTrackingNumber_returns200AndBody() throws Exception {
        // Arrange
        var trackingNumber = "ABC123XYZ";
        var dto = new TrackingNumberRecords.TrackingDetailData(
                trackingNumber,
                "MY", "ID",
                new BigDecimal("2.345"),
                Instant.parse("2025-06-26T09:00:00Z"),
                UUID.fromString("de619854-b59b-425e-9db4-943979e1bd49"),
                "Acme Corp",
                "acme-corp",
                Instant.parse("2025-06-26T09:01:00Z"),
                "IN_TRANSIT",
                Map.of("fragile", true)
        );
        when(service.findByTrackingNumber(trackingNumber)).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/track/{tn}", trackingNumber)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tracking_number").value(trackingNumber))
                .andExpect(jsonPath("$.origin_country_id").value("MY"))
                .andExpect(jsonPath("$.destination_country_id").value("ID"))
                .andExpect(jsonPath("$.weight").value(2.345))
                .andExpect(jsonPath("$.generated_at").value("2025-06-26T09:01:00Z"))
                .andExpect(jsonPath("$.customer_id").value("de619854-b59b-425e-9db4-943979e1bd49"))
                .andExpect(jsonPath("$.customer_name").value("Acme Corp"))
                .andExpect(jsonPath("$.customer_slug").value("acme-corp"))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.metadata.fragile").value(true));

        // verify delegation
        verify(service).findByTrackingNumber(trackingNumber);
    }

    @Test
    @DisplayName("GET /api/v1/track/{tracking_number} – not found → 404 + ApiError")
    void detail_nonExistentTrackingNumber_returns404AndErrorBody() throws Exception {
        // Arrange
        var trackingNumber = "NONEXISTENT";
        when(service.findByTrackingNumber(trackingNumber))
                .thenThrow(new ResourceNotFoundException("TrackingNumber", trackingNumber));

        // Act & Assert
        mockMvc.perform(get("/api/v1/track/{tn}", trackingNumber)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("not found with identifier " + trackingNumber)))
                .andExpect(jsonPath("$.path").value("/api/v1/track/" + trackingNumber));

        verify(service).findByTrackingNumber(trackingNumber);
    }

    @Test
    @DisplayName("GET /api/v1/track/{tracking_number} – invalid format → 400 + ApiError")
    void detail_invalidTrackingNumberFormat_returns400AndErrorBody() throws Exception {
        var badTrackingNumber = "bad_slug!";

        mockMvc.perform(get("/api/v1/track/{tn}", badTrackingNumber)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field", containsString("trackingNumber")))
                .andExpect(jsonPath("$.path").value("/api/v1/track/" + badTrackingNumber));

        verifyNoInteractions(service);
    }

}