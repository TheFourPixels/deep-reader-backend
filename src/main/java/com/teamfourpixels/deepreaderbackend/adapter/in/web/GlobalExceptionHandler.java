package com.teamfourpixels.deepreaderbackend.adapter.in.web;

import com.teamfourpixels.deepreaderbackend.adapter.in.web.dto.ErrorCode;
import com.teamfourpixels.deepreaderbackend.adapter.in.web.dto.ErrorDetail;
import com.teamfourpixels.deepreaderbackend.adapter.in.web.dto.ErrorResponse;
import com.teamfourpixels.deepreaderbackend.domain.exception.EmptyAiResponseException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ExternalProviderException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ProviderTimeoutException;
import com.teamfourpixels.deepreaderbackend.domain.exception.TextTooLongException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getTraceId() {
        String traceId = MDC.get("trace_id");
        return (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.add(new ErrorDetail(fe.getField(), fe.getDefaultMessage()));
        }

        log.atWarn()
           .log("Ошибка валидации запроса: fields={}", details);

        ErrorResponse body = new ErrorResponse(
                ErrorCode.INVALID_INPUT,
                "Ошибка валидации входных данных",
                Instant.now().toString(),
                getTraceId(),
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        log.atWarn().log("Ошибка валидации параметров запроса: {}", ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.INVALID_INPUT,
                "Ошибка валидации параметров запроса",
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        log.atWarn().log("Некорректный формат JSON тела запроса: {}", ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.INVALID_INPUT,
                "Некорректный формат JSON в теле запроса",
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(TextTooLongException.class)
    public ResponseEntity<ErrorResponse> handleTextTooLong(TextTooLongException ex) {
        log.atWarn().log("Превышен лимит токенов контекста: {}", ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.TEXT_TOO_LONG,
                ex.getMessage(),
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(EmptyAiResponseException.class)
    public ResponseEntity<ErrorResponse> handleEmptyAiResponse(EmptyAiResponseException ex) {
        log.atWarn().log("Пустой ответ от AI провайдера: {}", ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.EMPTY_AI_RESPONSE,
                ex.getMessage(),
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(body);
    }

    @ExceptionHandler(ProviderTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleProviderTimeout(ProviderTimeoutException ex) {
        log.atError()
           .setCause(ex)
           .log("Превышен таймаут вызова внешнего провайдера '{}': {}", ex.getProvider(), ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.EXTERNAL_PROVIDER_TIMEOUT,
                ex.getMessage(),
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException ex) {
        log.atError()
           .setCause(ex)
           .log("Сетевой таймаут / ошибка доступа к ресурсу: {}", ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.EXTERNAL_PROVIDER_TIMEOUT,
                "Превышен таймаут ожидания ответа внешнего сервиса: " + ex.getMessage(),
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCallNotPermitted(CallNotPermittedException ex) {
        log.atError().setCause(ex).log("Circuit breaker open: {}", ex.getCausingCircuitBreakerName());
        boolean isDeepL = "deepl".equalsIgnoreCase(ex.getCausingCircuitBreakerName());
        ErrorCode code = isDeepL ? ErrorCode.TRANSLATION_FAILED : ErrorCode.LLM_PROVIDER_ERROR;
        String message = isDeepL
                ? "Сервис перевода временно недоступен (circuit breaker open)"
                : "Сервис LLM временно недоступен (circuit breaker open)";

        ErrorResponse body = new ErrorResponse(
                code,
                message,
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(ExternalProviderException.class)
    public ResponseEntity<ErrorResponse> handleExternalProviderException(ExternalProviderException ex) {
        log.atError()
           .setCause(ex)
           .log("Сбой на стороне внешнего провайдера '{}': {}", ex.getProvider(), ex.getMessage());

        ErrorCode code = "deepl".equalsIgnoreCase(ex.getProvider())
                ? ErrorCode.TRANSLATION_FAILED
                : ErrorCode.LLM_PROVIDER_ERROR;

        ErrorResponse body = new ErrorResponse(
                code,
                ex.getMessage(),
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.atWarn().log("Маршрут не найден: {}", ex.getResourcePath());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.INVALID_INPUT,
                "Ресурс " + ex.getResourcePath() + " не найден",
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllOtherExceptions(Exception ex) {
        log.atError()
           .setCause(ex)
           .log("Непредвиденное внутреннее исключение: {}", ex.getMessage());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR,
                "Внутренняя непредвиденная ошибка сервера",
                Instant.now().toString(),
                getTraceId(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
