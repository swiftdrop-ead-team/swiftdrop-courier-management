package com.swiftdrop.parcel.enums;

import java.util.EnumSet;
import java.util.Set;

public enum ParcelStatus {
    CREATED,
    PICKED_UP,
    RECEIVED_AT_HUB,
    SORTED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    CANCELLED;

    public boolean canTransitionTo(ParcelStatus target) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case CREATED -> EnumSet.of(PICKED_UP, CANCELLED).contains(target);
            case PICKED_UP -> EnumSet.of(RECEIVED_AT_HUB, CANCELLED).contains(target);
            case RECEIVED_AT_HUB -> EnumSet.of(SORTED, CANCELLED).contains(target);
            case SORTED -> EnumSet.of(IN_TRANSIT).contains(target);
            case IN_TRANSIT -> EnumSet.of(OUT_FOR_DELIVERY, RECEIVED_AT_HUB, FAILED).contains(target);
            case OUT_FOR_DELIVERY -> EnumSet.of(DELIVERED, FAILED).contains(target);
            case FAILED -> EnumSet.of(OUT_FOR_DELIVERY, RECEIVED_AT_HUB, CANCELLED).contains(target);
            case DELIVERED, CANCELLED -> false; // Terminal states
        };
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public boolean isCancellable() {
        return this == CREATED || this == PICKED_UP || this == RECEIVED_AT_HUB;
    }
}

