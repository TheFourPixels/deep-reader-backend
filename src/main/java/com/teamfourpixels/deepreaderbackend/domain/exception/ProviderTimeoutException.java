package com.teamfourpixels.deepreaderbackend.domain.exception;

public class ProviderTimeoutException extends RuntimeException {
    private final String provider;

    public ProviderTimeoutException(String provider, String message) {
        super(message);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
