package com.teamfourpixels.deepreaderbackend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeepReaderBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void initDb() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS ai_cache_entries (
                hash_key VARCHAR(64) PRIMARY KEY,
                cache_type VARCHAR(16) NOT NULL,
                response_data TEXT NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP WITH TIME ZONE NOT NULL
            )
        """);
    }

    @Test
    void testContextLoadsAndHealthEndpointWorks() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("deep-reader-backend")))
                .andExpect(jsonPath("$.active_llm_provider", is("MockLlmAdapter")));
    }

    @Test
    void testExplainEndToEndWithMockLlmAdapter() throws Exception {
        String requestJson = """
                {
                  "book_id": "test-book-uuid",
                  "selected_text": "cold day in April",
                  "current_sentence": "It was a bright cold day in April, and the clocks were striking thirteen.",
                  "surrounding_context": {
                    "sentences_before": ["The hallway smelt of boiled cabbage."],
                    "sentences_after": ["Winston Smith slipped quickly through the glass doors."],
                    "paragraph": "Full paragraph text.",
                    "chapter_context": "Chapter 1"
                  },
                  "target_language": "ru"
                }
                """;

        mockMvc.perform(post("/api/v1/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected_text", is("cold day in April")))
                .andExpect(jsonPath("$.phenomenon_type", notNullValue()))
                .andExpect(jsonPath("$.explanation", containsString("Mock")))
                .andExpect(jsonPath("$.cached", is(false)));

        mockMvc.perform(post("/api/v1/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached", is(true)));
    }

    @Test
    void testTranslateEndToEndWithMockTranslationAdapter() throws Exception {
        String requestJson = """
                {
                  "text": "The sky was dark",
                  "source_language": "en",
                  "target_language": "ru"
                }
                """;

        mockMvc.perform(post("/api/v1/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translated_text", containsString("[MOCK ПЕРЕВОД]")))
                .andExpect(jsonPath("$.cached", is(false)));

        mockMvc.perform(post("/api/v1/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached", is(true)));
    }
}
