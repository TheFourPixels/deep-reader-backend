package com.teamfourpixels.deepreaderbackend.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorDetail(
        @JsonProperty("field") String field,
        @JsonProperty("issue") String issue
) {}
