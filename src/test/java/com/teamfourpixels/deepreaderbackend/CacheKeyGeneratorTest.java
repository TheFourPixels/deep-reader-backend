package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.domain.model.ExplainRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.SurroundingContext;
import com.teamfourpixels.deepreaderbackend.domain.service.CacheKeyGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CacheKeyGeneratorTest {

    private final CacheKeyGenerator generator = new CacheKeyGenerator("v1.0");

    @Test
    void testSha256LengthIsExactly64Characters() {
        SurroundingContext ctx = new SurroundingContext(
                List.of("Sentence 1"), List.of("Sentence 2"), "Paragraph", "Chapter"
        );
        ExplainRequestDomain req = new ExplainRequestDomain(
                "1984-george-orwell",
                "clocks were striking thirteen",
                "It was a bright cold day in April, and the clocks were striking thirteen.",
                ctx,
                "ru"
        );

        String explainKey = generator.forExplain(req);
        assertEquals(64, explainKey.length(), "Explain cache key must be exactly 64 characters long to fit VARCHAR(64)");

        String translateKey = generator.forTranslate("Hello world", "en", "ru");
        assertEquals(64, translateKey.length(), "Translate cache key must be exactly 64 characters long to fit VARCHAR(64)");
    }

    @Test
    void testDeterminismAndNormalization() {
        SurroundingContext ctx1 = new SurroundingContext(
                List.of("Before"), List.of("After"), "Para   with   extra   spaces", null
        );
        SurroundingContext ctx2 = new SurroundingContext(
                List.of("Before"), List.of("After"), "Para with extra spaces", null
        );

        ExplainRequestDomain req1 = new ExplainRequestDomain("book", "test \u200B text", "Sentence", ctx1, "ru");
        ExplainRequestDomain req2 = new ExplainRequestDomain("book", "test text", "Sentence", ctx2, "ru");

        String key1 = generator.forExplain(req1);
        String key2 = generator.forExplain(req2);

        assertEquals(key1, key2, "Normalized text and collapsed whitespace must produce the same SHA-256 key");
    }

    @Test
    void testTargetLanguageDifferentiatesExplainKey() {
        SurroundingContext ctx = new SurroundingContext(
                List.of("Sentence 1"), List.of("Sentence 2"), "Paragraph", "Chapter"
        );
        ExplainRequestDomain reqRu = new ExplainRequestDomain(
                "1984-george-orwell",
                "clocks were striking thirteen",
                "It was a bright cold day in April.",
                ctx,
                "ru"
        );
        ExplainRequestDomain reqEn = new ExplainRequestDomain(
                "1984-george-orwell",
                "clocks were striking thirteen",
                "It was a bright cold day in April.",
                ctx,
                "en"
        );

        String keyRu = generator.forExplain(reqRu);
        String keyEn = generator.forExplain(reqEn);

        assertNotEquals(keyRu, keyEn, "Keys for different target languages must differ to prevent cache cross-contamination");
    }
}
