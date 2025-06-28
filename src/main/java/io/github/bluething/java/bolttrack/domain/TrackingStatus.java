package io.github.bluething.java.bolttrack.domain;

import java.util.EnumSet;
import java.util.Set;

public enum TrackingStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION,
    RETURNED,
    CANCELLED;

    private static final Set<TrackingStatus> CREATED_NEXT       = EnumSet.of(PICKED_UP, CANCELLED);
    private static final Set<TrackingStatus> PICKED_UP_NEXT     = EnumSet.of(IN_TRANSIT, EXCEPTION);
    private static final Set<TrackingStatus> IN_TRANSIT_NEXT    = EnumSet.of(OUT_FOR_DELIVERY, EXCEPTION, RETURNED);
    private static final Set<TrackingStatus> OUT_FOR_DELIVERY_NEXT = EnumSet.of(DELIVERED, EXCEPTION);
    private static final Set<TrackingStatus> EXCEPTION_NEXT     = EnumSet.of(IN_TRANSIT, RETURNED);
    private static final Set<TrackingStatus> FINAL_STATUSES     = EnumSet.of(DELIVERED, RETURNED, CANCELLED);

    /**
     * Returns true if this status may validly transition to {@code next}.
     */
    public boolean canTransitionTo(TrackingStatus next) {
        if (this == CREATED)          return CREATED_NEXT.contains(next);
        if (this == PICKED_UP)        return PICKED_UP_NEXT.contains(next);
        if (this == IN_TRANSIT)       return IN_TRANSIT_NEXT.contains(next);
        if (this == OUT_FOR_DELIVERY) return OUT_FOR_DELIVERY_NEXT.contains(next);
        if (this == EXCEPTION)        return EXCEPTION_NEXT.contains(next);
        // once in a final state, no further transitions allowed
        return false;
    }

    public boolean isFinal() {
        return FINAL_STATUSES.contains(this);
    }
}
