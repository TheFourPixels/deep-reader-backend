package com.teamfourpixels.deepreaderbackend.adapter.out.llm;

import com.teamfourpixels.deepreaderbackend.domain.model.*;
import com.teamfourpixels.deepreaderbackend.port.out.LlmPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.llm.provider", havingValue = "mock")
public class MockLlmAdapter implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(MockLlmAdapter.class);

    @Override
    public ExplainResponseDomain explain(ExplainRequestDomain request) {
        log.atInfo().log("[MOCK LLM] Возвращаю mock-ответ для: '{}'", request.selectedText());
        return new ExplainResponseDomain(
                request.selectedText(),
                "Mock: контекстное значение выражения в данной сцене.",
                PhenomenonType.METAPHOR,
                "Mock: подробное объяснение. Это тестовый ответ без реального вызова OpenAI.",
                StylisticColoring.LITERARY,
                false,
                false,
                42L
        );
    }
}
