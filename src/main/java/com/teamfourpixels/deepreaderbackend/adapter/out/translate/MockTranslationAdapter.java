package com.teamfourpixels.deepreaderbackend.adapter.out.translate;

import com.teamfourpixels.deepreaderbackend.domain.model.TranslationRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;
import com.teamfourpixels.deepreaderbackend.port.out.TranslationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.translator.provider", havingValue = "mock")
public class MockTranslationAdapter implements TranslationPort {

    private static final Logger log = LoggerFactory.getLogger(MockTranslationAdapter.class);

    @Override
    public TranslationResponseDomain translate(TranslationRequestDomain request) {
        log.atInfo().log("[MOCK Translate] text='{}', target='{}'", request.text(), request.targetLanguage());
        return new TranslationResponseDomain(
                "[MOCK ПЕРЕВОД]: " + request.text(),
                request.sourceLanguage() != null ? request.sourceLanguage() : "en",
                request.targetLanguage(),
                false
        );
    }
}
