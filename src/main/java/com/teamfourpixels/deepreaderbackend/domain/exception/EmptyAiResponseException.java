package com.teamfourpixels.deepreaderbackend.domain.exception;

public class EmptyAiResponseException extends RuntimeException {
    public EmptyAiResponseException(String message) {
        super(message);
    }
}
