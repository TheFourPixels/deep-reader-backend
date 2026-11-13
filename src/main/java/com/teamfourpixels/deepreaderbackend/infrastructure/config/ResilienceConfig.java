package com.teamfourpixels.deepreaderbackend.infrastructure.config;

import com.teamfourpixels.deepreaderbackend.domain.exception.EmptyAiResponseException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ExternalProviderException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ProviderTimeoutException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean("openAiCircuitBreaker")
    public CircuitBreaker openAiCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(ExternalProviderException.class, ProviderTimeoutException.class, ResourceAccessException.class)
                .ignoreExceptions(EmptyAiResponseException.class)
                .build();
        return CircuitBreaker.of("openai", config);
    }

    @Bean("openAiRetry")
    public Retry openAiRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(500), 1.5, 0.5))
                .retryExceptions(ExternalProviderException.class, ResourceAccessException.class)
                .ignoreExceptions(EmptyAiResponseException.class, ProviderTimeoutException.class)
                .build();
        return Retry.of("openai", config);
    }

    @Bean("deepLCircuitBreaker")
    public CircuitBreaker deepLCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(ExternalProviderException.class, ProviderTimeoutException.class, ResourceAccessException.class)
                .build();
        return CircuitBreaker.of("deepl", config);
    }

    @Bean("deepLRetry")
    public Retry deepLRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(300), 1.5, 0.5))
                .retryExceptions(ExternalProviderException.class, ResourceAccessException.class)
                .ignoreExceptions(ProviderTimeoutException.class)
                .build();
        return Retry.of("deepl", config);
    }
}
