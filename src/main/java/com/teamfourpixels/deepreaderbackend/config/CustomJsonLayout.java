package com.teamfourpixels.deepreaderbackend.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;

import java.time.Instant;
import java.util.Map;

public class CustomJsonLayout extends LayoutBase<ILoggingEvent> {

    private String serviceName = "deep-reader-backend";

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');

        sb.append("\"timestamp\":");
        escapeJsonString(sb, Instant.ofEpochMilli(event.getTimeStamp()).toString());

        sb.append(",\"level\":");
        escapeJsonString(sb, event.getLevel().toString());

        sb.append(",\"service\":");
        escapeJsonString(sb, serviceName);

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            String traceId = mdc.get("trace_id");
            if (traceId != null) {
                sb.append(",\"trace_id\":");
                escapeJsonString(sb, traceId);
            }
            String spanId = mdc.get("span_id");
            if (spanId != null) {
                sb.append(",\"span_id\":");
                escapeJsonString(sb, spanId);
            }
        }

        sb.append(",\"thread\":");
        escapeJsonString(sb, event.getThreadName());

        sb.append(",\"message\":");
        escapeJsonString(sb, event.getFormattedMessage());

        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            String className = throwableProxy.getClassName();
            int lastDot = className != null ? className.lastIndexOf('.') : -1;
            String simpleName = (lastDot >= 0 && className != null) ? className.substring(lastDot + 1) : className;

            sb.append(",\"error\":{");
            sb.append("\"type\":");
            escapeJsonString(sb, simpleName);
            sb.append(",\"message\":");
            escapeJsonString(sb, throwableProxy.getMessage());
            sb.append(",\"stack_trace\":");
            escapeJsonString(sb, ThrowableProxyUtil.asString(throwableProxy));
            sb.append('}');
        }

        if (mdc != null && mdc.containsKey("request_path")) {
            sb.append(",\"context\":{");
            sb.append("\"request_path\":");
            escapeJsonString(sb, mdc.get("request_path"));
            sb.append(",\"http_method\":");
            escapeJsonString(sb, mdc.get("http_method"));
            sb.append('}');
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static void escapeJsonString(StringBuilder sb, String s) {
        if (s == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
