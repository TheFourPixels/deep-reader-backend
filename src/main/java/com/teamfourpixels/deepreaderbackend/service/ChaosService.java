package com.teamfourpixels.deepreaderbackend.service;

import com.teamfourpixels.deepreaderbackend.exception.ChaosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChaosService {

    private static final Logger log = LoggerFactory.getLogger(ChaosService.class);

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicReference<Double> errorRate = new AtomicReference<>(0.0);
    private final AtomicInteger delayMs = new AtomicInteger(0);

    public void triggerFault(double rate, int delay) {
        double boundedRate = Math.max(0.0, Math.min(1.0, rate));
        int boundedDelay = Math.max(0, delay);
        this.errorRate.set(boundedRate);
        this.delayMs.set(boundedDelay);
        this.enabled.set(true);
        log.warn("Fault mode enabled: errorRate={}, delayMs={}", boundedRate, boundedDelay);
    }

    public void reset() {
        this.enabled.set(false);
        this.errorRate.set(0.0);
        this.delayMs.set(0);
        log.info("Fault mode reset: connection pool restored");
    }

    public Map<String, Object> getStatus() {
        return Map.of(
                "enabled", enabled.get(),
                "errorRate", errorRate.get(),
                "delayMs", delayMs.get(),
                "description", enabled.get()
                        ? "Fault mode active: errorRate=" + (int)(errorRate.get() * 100) + "%, delay=" + delayMs.get() + "ms"
                        : "Normal operation"
        );
    }

    public void applyChaosIfNeeded() {
        if (!enabled.get()) {
            return;
        }

        int delay = delayMs.get();
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        double rate = errorRate.get();
        if (rate > 0.0 && ThreadLocalRandom.current().nextDouble() < rate) {
            log.warn("Database connection timeout: pool exhausted");
            throw new ChaosException("Database connection timeout: pool exhausted");
        }
    }
}
