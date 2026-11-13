package com.teamfourpixels.deepreaderbackend;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.LoggerContext;
import com.teamfourpixels.deepreaderbackend.config.CustomJsonLayout;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class CustomJsonLayoutTest {

    @Test
    void testCustomJsonLayoutFormatsExactExpectedSchema() {
        CustomJsonLayout layout = new CustomJsonLayout();
        layout.setServiceName("deep-reader-backend");
        layout.start();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger("com.teamfourpixels.deepreaderbackend.adapter.in.web.GlobalExceptionHandler");

        MDC.put("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        MDC.put("span_id", "00f067aa0ba902b7");
        MDC.put("request_path", "/api/v1/explain");
        MDC.put("http_method", "POST");

        try {
            Exception fakeException = new IllegalArgumentException("Validation failed for argument [0] in public ExplainResponse...");
            LoggingEvent event = new LoggingEvent(
                    "com.teamfourpixels.deepreaderbackend.adapter.in.web.GlobalExceptionHandler",
                    logger,
                    Level.ERROR,
                    "Failed to process explain: validation failed",
                    fakeException,
                    null
            );

            String jsonOutput = layout.doLayout(event);
            System.out.println("Generated JSON Log:\n" + jsonOutput);

            assertTrue(jsonOutput.contains("\"service\":\"deep-reader-backend\""));
            assertTrue(jsonOutput.contains("\"level\":\"ERROR\""));
            assertTrue(jsonOutput.contains("\"trace_id\":\"4bf92f3577b34da6a3ce929d0e0e4736\""));
            assertTrue(jsonOutput.contains("\"span_id\":\"00f067aa0ba902b7\""));
            assertTrue(jsonOutput.contains("\"message\":\"Failed to process explain: validation failed\""));
            assertTrue(jsonOutput.contains("\"error\":{"));
            assertTrue(jsonOutput.contains("\"type\":\"IllegalArgumentException\""));
            assertTrue(jsonOutput.contains("\"context\":{"));
            assertTrue(jsonOutput.contains("\"request_path\":\"/api/v1/explain\""));
            assertTrue(jsonOutput.contains("\"http_method\":\"POST\""));
        } finally {
            MDC.clear();
            layout.stop();
        }
    }
}
