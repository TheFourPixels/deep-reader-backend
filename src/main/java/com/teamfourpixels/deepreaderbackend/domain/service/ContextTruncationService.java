package com.teamfourpixels.deepreaderbackend.domain.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.teamfourpixels.deepreaderbackend.adapter.out.llm.PromptTemplates;
import com.teamfourpixels.deepreaderbackend.domain.exception.TextTooLongException;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.SurroundingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContextTruncationService {

    private final Encoding encoding;
    private final int maxContextTokens;

    @Autowired
    public ContextTruncationService(
            @Value("${app.ai.max-context-tokens:1000}") int maxContextTokens) {
        this(maxContextTokens, true);
    }

    public ContextTruncationService(int maxContextTokens, boolean accountForSystemPrompt) {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.O200K_BASE);
        int sysPromptTokens = accountForSystemPrompt ? this.encoding.countTokens(PromptTemplates.SYSTEM_PROMPT) : 0;
        this.maxContextTokens = Math.max(1, maxContextTokens - sysPromptTokens);
    }

    public int getMaxContextTokens() {
        return maxContextTokens;
    }

    public ExplainRequestDomain truncate(ExplainRequestDomain request) {
        int coreTokens = countTokens(request.selectedText() + " " + request.currentSentence());
        if (coreTokens > maxContextTokens) {
            throw new TextTooLongException(
                    "Выделенный текст вместе с текущим предложением превышает допустимый лимит токенов (%d токенов). Сократите выделение."
                            .formatted(maxContextTokens));
        }

        SurroundingContext ctx = request.surroundingContext();
        List<String> before       = new ArrayList<>(ctx.sentencesBefore());
        List<String> after        = new ArrayList<>(ctx.sentencesAfter());
        String       paragraph    = ctx.paragraph();
        String       chapterCtx   = ctx.chapterContext();

        if (totalTokens(request.selectedText(), request.currentSentence(), before, after, paragraph, chapterCtx) <= maxContextTokens) {
            return request;
        }

        chapterCtx = null;
        if (totalTokens(request.selectedText(), request.currentSentence(), before, after, paragraph, null) <= maxContextTokens) {
            return rebuild(request, before, after, paragraph, null);
        }

        if (before.size() >= 2) {
            before.remove(0);
            if (totalTokens(request.selectedText(), request.currentSentence(), before, after, paragraph, null) <= maxContextTokens) {
                return rebuild(request, before, after, paragraph, null);
            }
        }

        if (after.size() >= 2) {
            after.remove(1);
            if (totalTokens(request.selectedText(), request.currentSentence(), before, after, paragraph, null) <= maxContextTokens) {
                return rebuild(request, before, after, paragraph, null);
            }
        }

        if (!after.isEmpty()) {
            after.remove(0);
            if (totalTokens(request.selectedText(), request.currentSentence(), before, after, paragraph, null) <= maxContextTokens) {
                return rebuild(request, before, after, paragraph, null);
            }
        }

        if (!before.isEmpty()) {
            before.remove(0);
            if (totalTokens(request.selectedText(), request.currentSentence(), before, after, paragraph, null) <= maxContextTokens) {
                return rebuild(request, before, after, paragraph, null);
            }
        }

        paragraph = null;
        return rebuild(request, List.of(), List.of(), null, null);
    }

    private int totalTokens(String selected, String sentence,
                             List<String> before, List<String> after,
                             String paragraph, String chapterCtx) {
        StringBuilder sb = new StringBuilder();
        sb.append(selected).append(" ").append(sentence);
        before.forEach(s -> sb.append(" ").append(s));
        after.forEach(s  -> sb.append(" ").append(s));
        if (paragraph   != null) sb.append(" ").append(paragraph);
        if (chapterCtx  != null) sb.append(" ").append(chapterCtx);
        return countTokens(sb.toString());
    }

    private int countTokens(String text) {
        return encoding.countTokens(text);
    }

    private ExplainRequestDomain rebuild(ExplainRequestDomain original,
                                          List<String> before,
                                          List<String> after,
                                          String paragraph,
                                          String chapterCtx) {
        SurroundingContext truncated = new SurroundingContext(before, after, paragraph, chapterCtx);
        return new ExplainRequestDomain(
                original.bookId(),
                original.selectedText(),
                original.currentSentence(),
                truncated,
                original.targetLanguage()
        );
    }
}
