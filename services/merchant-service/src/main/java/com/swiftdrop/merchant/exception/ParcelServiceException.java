package com.swiftdrop.merchant.exception;

public class ParcelServiceException extends RuntimeException {

    public ParcelServiceException(String message) {
        super(message);
    }

    public ParcelServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
