package io.github.bluething.java.bolttrack.rest;

import io.github.bluething.java.bolttrack.domain.TrackingNumberRecords;
import io.github.bluething.java.bolttrack.domain.TrackingNumberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
class TrackingNumberController {
    private final TrackingNumberService trackingNumberService;

    @GetMapping("/next-tracking-number")
    public TrackingNumberResponse nextViaGet(@Valid @ModelAttribute TrackingNumberRequest request) {
        TrackingNumberRecords.TrackingNumberData dto = trackingNumberService.generate(TrackingRestMapper.toDto(request));
        return TrackingRestMapper.toRest(dto);
    }
}
