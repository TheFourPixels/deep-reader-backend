package com.teamfourpixels.deepreaderbackend.adapter.in.web.dto;

public enum ErrorCode {
    INVALID_INPUT,
    TEXT_TOO_LONG,
    EMPTY_AI_RESPONSE,
    TRANSLATION_FAILED,
    LLM_PROVIDER_ERROR,
    EXTERNAL_PROVIDER_TIMEOUT,
    RATE_LIMIT_EXCEEDED,
    INTERNAL_ERROR
}
