package com.teamfourpixels.deepreaderbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader("X-B3-TraceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader("X-Trace-Id");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        String spanId = request.getHeader("X-B3-SpanId");
        if (spanId == null || spanId.isBlank()) {
            spanId = request.getHeader("X-Span-Id");
        }
        if (spanId == null || spanId.isBlank()) {
            spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        try {
            MDC.put("trace_id", traceId);
            MDC.put("span_id", spanId);
            MDC.put("request_path", request.getRequestURI());
            MDC.put("http_method", request.getMethod());

            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Span-Id", spanId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
