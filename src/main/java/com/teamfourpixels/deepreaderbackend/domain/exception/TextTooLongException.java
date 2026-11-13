package com.teamfourpixels.deepreaderbackend.domain.exception;

public class TextTooLongException extends RuntimeException {
    public TextTooLongException(String message) {
        super(message);
    }
}
