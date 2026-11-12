package com.teamfourpixels.deepreaderbackend;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeepReaderBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void contextLoadsAndHealthCheckWorks() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("deep-reader-backend")));

        mockMvc.perform(get("/non-existent-resource.xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @Order(2)
    void testBookCrudOperations() throws Exception {
        String newBookJson = """
                {
                    \"title\": \"Neuromancer\",
                    \"author\": \"William Gibson\",
                    \"genre\": \"Cyberpunk\",
                    \"language\": \"en\",
                    \"description\": \"The sky above the port was the color of television, tuned to a dead channel.\"
                }
                """;

        String response = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newBookJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Neuromancer")))
                .andExpect(jsonPath("$.author", is("William Gibson")))
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = Pattern.compile("\"id\":\\s*(\\d+)").matcher(response);
        assertTrue(matcher.find(), "ID should be present in response");
        long createdBookId = Long.parseLong(matcher.group(1));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));

        mockMvc.perform(get("/api/books/" + createdBookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Neuromancer")));

        String updateBookJson = """
                {
                    \"title\": \"Neuromancer (Sprawl Trilogy Book 1)\",
                    \"author\": \"William Gibson\",
                    \"genre\": \"Sci-Fi Cyberpunk\",
                    \"language\": \"en\",
                    \"description\": \"Updated description\"
                }
                """;

        mockMvc.perform(put("/api/books/" + createdBookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBookJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Neuromancer (Sprawl Trilogy Book 1)")));

        mockMvc.perform(delete("/api/books/" + createdBookId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/" + createdBookId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    void testChapterAndAnnotationCrud() throws Exception {
        String bookJson = """
                {
                    \"title\": \"1984\",
                    \"author\": \"George Orwell\",
                    \"genre\": \"Dystopian\",
                    \"language\": \"en\",
                    \"description\": \"Classic dystopian novel.\"
                }
                """;

        String bookResponse = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Matcher bookMatcher = Pattern.compile("\"id\":\\s*(\\d+)").matcher(bookResponse);
        assertTrue(bookMatcher.find());
        long bookId = Long.parseLong(bookMatcher.group(1));

        String chapterJson = """
                {
                    \"chapterNumber\": 1,
                    \"title\": \"It was a bright cold day in April\",
                    \"content\": \"It was a bright cold day in April, and the clocks were striking thirteen. Big Brother is watching you.\"
                }
                """;

        String chapterResponse = mockMvc.perform(post("/api/books/" + bookId + "/chapters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chapterJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chapterNumber", is(1)))
                .andReturn().getResponse().getContentAsString();

        Matcher chapterMatcher = Pattern.compile("\"id\":\\s*(\\d+)").matcher(chapterResponse);
        assertTrue(chapterMatcher.find());
        long chapterId = Long.parseLong(chapterMatcher.group(1));

        String annotationJson = String.format("""
                {
                    \"chapterId\": %d,
                    \"targetPhrase\": \"clocks were striking thirteen\",
                    \"explanation\": \"Сюрреалистическая метафора искаженной реальности тоталитарного государства.\",
                    \"type\": \"METAPHOR\",
                    \"contextSnippet\": \"It was a bright cold day in April, and the clocks were striking thirteen.\"
                }
                """, chapterId);

        String annotationResponse = mockMvc.perform(post("/api/books/" + bookId + "/annotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetPhrase", is("clocks were striking thirteen")))
                .andExpect(jsonPath("$.type", is("METAPHOR")))
                .andReturn().getResponse().getContentAsString();

        Matcher annMatcher = Pattern.compile("\"id\":\\s*(\\d+)").matcher(annotationResponse);
        assertTrue(annMatcher.find());
        long annotationId = Long.parseLong(annMatcher.group(1));

        mockMvc.perform(get("/api/chapters/" + chapterId + "/annotations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetPhrase", is("clocks were striking thirteen")));

        String updateAnnotationJson = String.format("""
                {
                    \"chapterId\": %d,
                    \"targetPhrase\": \"clocks were striking thirteen\",
                    \"explanation\": \"Обновленное объяснение метафоры Оруэлла.\",
                    \"type\": \"ALLUSION\",
                    \"contextSnippet\": \"It was a bright cold day in April, and the clocks were striking thirteen.\"
                }
                """, chapterId);

        mockMvc.perform(put("/api/annotations/" + annotationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateAnnotationJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("ALLUSION")));

        mockMvc.perform(delete("/api/annotations/" + annotationId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OK")));
    }
}
