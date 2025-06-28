package io.github.bluething.java.bolttrack.rest;

import io.github.bluething.java.bolttrack.domain.TrackingNumberRecords;
import io.github.bluething.java.bolttrack.domain.TrackingNumberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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

    @GetMapping("/track/{tracking_number}")
    public TrackingDetailResponse detail(
            @PathVariable("tracking_number")
            @Pattern(regexp = "^[0-9A-Z]{1,16}$",
                    message = "tracking_number must be 1–16 chars [0-9A-Z]")
            String trackingNumber
    ) {
        var detailDto = trackingNumberService.findByTrackingNumber(trackingNumber);
        return TrackingRestMapper.toDetailRest(detailDto);
    }
}
