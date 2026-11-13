package com.teamfourpixels.deepreaderbackend.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @JsonProperty("error_code") ErrorCode errorCode,
        @JsonProperty("message")    String message,
        @JsonProperty("timestamp")  String timestamp,
        @JsonProperty("trace_id")   String traceId,
        @JsonProperty("details")    List<ErrorDetail> details
) {
    public ErrorResponse(ErrorCode errorCode, String message, String timestamp, String traceId) {
        this(errorCode, message, timestamp, traceId, null);
    }
}
