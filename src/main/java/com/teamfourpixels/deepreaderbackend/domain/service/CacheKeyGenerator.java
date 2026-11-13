package com.teamfourpixels.deepreaderbackend.domain.service;

import com.teamfourpixels.deepreaderbackend.domain.model.ExplainRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.SurroundingContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.List;

@Service
public class CacheKeyGenerator {

    private static final String SEPARATOR = ":";
    private final String promptVersion;

    public CacheKeyGenerator(@Value("${app.ai.prompt-version:v1.0}") String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String forExplain(ExplainRequestDomain request) {
        String fingerprint = buildContextFingerprint(request);
        String targetLang = request.targetLanguage() != null && !request.targetLanguage().isBlank()
                ? request.targetLanguage().toLowerCase()
                : "ru";
        String raw = promptVersion + SEPARATOR
                + request.bookId() + SEPARATOR
                + normalize(request.selectedText()) + SEPARATOR
                + normalize(fingerprint) + SEPARATOR
                + targetLang;
        return sha256(raw);
    }

    public String forTranslate(String text, String sourceLanguage, String targetLanguage) {
        String raw = "translate" + SEPARATOR
                + normalize(text) + SEPARATOR
                + (sourceLanguage != null ? sourceLanguage.toLowerCase() : "auto") + SEPARATOR
                + targetLanguage.toLowerCase();
        return sha256(raw);
    }

    static String normalize(String text) {
        if (text == null) return "";

        String nfkc = Normalizer.normalize(text, Normalizer.Form.NFKC);

        String cleaned = nfkc
                .replaceAll("[\\u200B\\u00AD\\p{Cc}&&[^\\n]]", "")

                .replaceAll("\\s+", " ")
                .strip();
        return cleaned;
    }

    private String buildContextFingerprint(ExplainRequestDomain request) {
        SurroundingContext ctx = request.surroundingContext();
        StringBuilder sb = new StringBuilder();
        sb.append(request.currentSentence());
        appendList(sb, ctx.sentencesBefore());
        appendList(sb, ctx.sentencesAfter());
        if (ctx.paragraph() != null)    sb.append("|").append(ctx.paragraph());
        if (ctx.chapterContext() != null) sb.append("|").append(ctx.chapterContext());
        return sb.toString();
    }

    private void appendList(StringBuilder sb, List<String> items) {
        for (String item : items) sb.append("|").append(item);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
