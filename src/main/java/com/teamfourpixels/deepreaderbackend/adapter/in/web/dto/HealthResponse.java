package com.teamfourpixels.deepreaderbackend.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        @JsonProperty("status")               String status,
        @JsonProperty("service")              String service,
        @JsonProperty("timestamp")            String timestamp,
        @JsonProperty("cache")                String cache,
        @JsonProperty("active_llm_provider")  String activeLlmProvider
) {}
