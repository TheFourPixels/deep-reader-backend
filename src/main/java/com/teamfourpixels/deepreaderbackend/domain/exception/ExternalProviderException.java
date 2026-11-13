package com.teamfourpixels.deepreaderbackend.domain.exception;

public class ExternalProviderException extends RuntimeException {
    private final String provider;

    public ExternalProviderException(String provider, String message) {
        super(message);
        this.provider = provider;
    }

    public ExternalProviderException(String provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
