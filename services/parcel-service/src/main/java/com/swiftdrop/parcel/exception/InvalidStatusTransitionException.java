package com.swiftdrop.parcel.exception;

import com.swiftdrop.parcel.enums.ParcelStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(ParcelStatus from, ParcelStatus to) {
        super(String.format("Invalid parcel status transition from %s to %s", from, to));
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}

