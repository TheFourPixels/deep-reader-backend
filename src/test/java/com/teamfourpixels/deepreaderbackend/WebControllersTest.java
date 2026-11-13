package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.adapter.in.web.ExplainController;
import com.teamfourpixels.deepreaderbackend.adapter.in.web.GlobalExceptionHandler;
import com.teamfourpixels.deepreaderbackend.adapter.in.web.SystemController;
import com.teamfourpixels.deepreaderbackend.adapter.in.web.TranslateController;
import com.teamfourpixels.deepreaderbackend.domain.exception.EmptyAiResponseException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ExternalProviderException;
import com.teamfourpixels.deepreaderbackend.domain.exception.ProviderTimeoutException;
import com.teamfourpixels.deepreaderbackend.domain.exception.TextTooLongException;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainResponseDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.PhenomenonType;
import com.teamfourpixels.deepreaderbackend.domain.model.StylisticColoring;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;
import com.teamfourpixels.deepreaderbackend.infrastructure.metrics.DeepReaderMetrics;
import com.teamfourpixels.deepreaderbackend.port.in.ExplainUseCase;
import com.teamfourpixels.deepreaderbackend.port.in.TranslateUseCase;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ExplainController.class,
        TranslateController.class,
        SystemController.class,
        GlobalExceptionHandler.class
})
class WebControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExplainUseCase explainUseCase;

    @MockitoBean
    private TranslateUseCase translateUseCase;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private DeepReaderMetrics metrics;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
    }

    @Test
    void testExplainSuccess200() throws Exception {
        ExplainResponseDomain domain = new ExplainResponseDomain(
                "clocks were striking thirteen",
                "Часы пробили тринадцать",
                PhenomenonType.METAPHOR,
                "Известная антиутопическая метафора Оруэлла",
                StylisticColoring.LITERARY,
                false,
                false,
                150L
        );
        when(explainUseCase.explain(any())).thenReturn(domain);

        String json = """
                {
                  "book_id": "1984",
                  "selected_text": "clocks were striking thirteen",
                  "current_sentence": "It was a bright cold day in April, and the clocks were striking thirteen.",
                  "surrounding_context": {
                    "sentences_before": [],
                    "sentences_after": []
                  },
                  "target_language": "ru"
                }
                """;

        mockMvc.perform(post("/api/v1/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected_text", is("clocks were striking thirteen")))
                .andExpect(jsonPath("$.contextual_meaning", is("Часы пробили тринадцать")))
                .andExpect(jsonPath("$.phenomenon_type", is("metaphor")))
                .andExpect(jsonPath("$.stylistic_coloring", is("literary")))
                .andExpect(jsonPath("$.cached", is(false)))
                .andExpect(jsonPath("$.processing_time_ms", is(150)));
    }

    @Test
    void testExplainValidationErrorMissingSelectedText400() throws Exception {
        String json = """
                {
                  "book_id": "1984",
                  "current_sentence": "Some sentence",
                  "surrounding_context": {
                    "sentences_before": [],
                    "sentences_after": []
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_INPUT")))
                .andExpect(jsonPath("$.message", containsString("Ошибка валидации")));
    }

    @Test
    void testExplainTextTooLong400() throws Exception {
        when(explainUseCase.explain(any()))
                .thenThrow(new TextTooLongException("Выделенный текст превышает допустимый лимит"));

        String json = """
                {
                  "book_id": "1984",
                  "selected_text": "some text",
                  "current_sentence": "Some sentence",
                  "surrounding_context": {
                    "sentences_before": [],
                    "sentences_after": []
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("TEXT_TOO_LONG")))
                .andExpect(jsonPath("$.message", is("Выделенный текст превышает допустимый лимит")));
    }

    @Test
    void testExplainEmptyAiResponse422() throws Exception {
        when(explainUseCase.explain(any()))
                .thenThrow(new EmptyAiResponseException("Провайдер AI вернул пустой ответ"));

        String json = """
                {
                  "book_id": "1984",
                  "selected_text": "some text",
                  "current_sentence": "Some sentence",
                  "surrounding_context": {
                    "sentences_before": [],
                    "sentences_after": []
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error_code", is("EMPTY_AI_RESPONSE")))
                .andExpect(jsonPath("$.message", is("Провайдер AI вернул пустой ответ")));
    }

    @Test
    void testExplainProviderTimeout504() throws Exception {
        when(explainUseCase.explain(any()))
                .thenThrow(new ProviderTimeoutException("openai", "Таймаут ожидания ответа OpenAI API (12s)"));

        String json = """
                {
                  "book_id": "1984",
                  "selected_text": "some text",
                  "current_sentence": "Some sentence",
                  "surrounding_context": {
                    "sentences_before": [],
                    "sentences_after": []
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error_code", is("EXTERNAL_PROVIDER_TIMEOUT")))
                .andExpect(jsonPath("$.message", containsString("Таймаут ожидания ответа")));
    }

    @Test
    void testExplainExternalProviderError502() throws Exception {
        when(explainUseCase.explain(any()))
                .thenThrow(new ExternalProviderException("openai", "Внутренняя ошибка OpenAI API (HTTP 500)"));

        String json = """
                {
                  "book_id": "1984",
                  "selected_text": "some text",
                  "current_sentence": "Some sentence",
                  "surrounding_context": {
                    "sentences_before": [],
                    "sentences_after": []
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error_code", is("LLM_PROVIDER_ERROR")))
                .andExpect(jsonPath("$.message", containsString("Внутренняя ошибка OpenAI API")));
    }

    @Test
    void testTranslateSuccess200() throws Exception {
        TranslationResponseDomain domain = new TranslationResponseDomain(
                "Привет, мир",
                "en",
                "ru",
                false
        );
        when(translateUseCase.translate(any())).thenReturn(domain);

        String json = """
                {
                  "text": "Hello world",
                  "source_language": "en",
                  "target_language": "ru"
                }
                """;

        mockMvc.perform(post("/api/v1/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translated_text", is("Привет, мир")))
                .andExpect(jsonPath("$.source_language", is("en")))
                .andExpect(jsonPath("$.target_language", is("ru")))
                .andExpect(jsonPath("$.cached", is(false)));
    }

    @Test
    void testTranslateValidationErrorEmptyText400() throws Exception {
        String json = """
                {
                  "text": "   ",
                  "target_language": "ru"
                }
                """;

        mockMvc.perform(post("/api/v1/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code", is("INVALID_INPUT")));
    }

    @Test
    void testTranslateCircuitBreakerOpen502() throws Exception {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("deepl");
        cb.transitionToOpenState();
        CallNotPermittedException openException = CallNotPermittedException.createCallNotPermittedException(cb);

        when(translateUseCase.translate(any())).thenThrow(openException);

        String json = """
                {
                  "text": "Hello world",
                  "target_language": "ru"
                }
                """;

        mockMvc.perform(post("/api/v1/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error_code", is("TRANSLATION_FAILED")))
                .andExpect(jsonPath("$.message", containsString("circuit breaker open")));
    }

    @Test
    void testHealthCheck200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("deep-reader-backend")))
                .andExpect(jsonPath("$.cache", is("CONNECTED")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void testHealthCheck503WhenDbDown() throws Exception {
        doThrow(new RuntimeException("Connection refused")).when(jdbcTemplate).execute(anyString());

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is("DOWN")))
                .andExpect(jsonPath("$.cache", containsString("DISCONNECTED")));
    }
}
