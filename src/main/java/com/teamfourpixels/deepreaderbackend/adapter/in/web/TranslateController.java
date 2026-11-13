package com.teamfourpixels.deepreaderbackend.adapter.in.web;

import com.teamfourpixels.deepreaderbackend.adapter.in.web.dto.TranslateRequest;
import com.teamfourpixels.deepreaderbackend.adapter.in.web.dto.TranslateResponse;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;
import com.teamfourpixels.deepreaderbackend.port.in.TranslateUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TranslateController {

    private static final Logger log = LoggerFactory.getLogger(TranslateController.class);

    private final TranslateUseCase translateUseCase;

    public TranslateController(TranslateUseCase translateUseCase) {
        this.translateUseCase = translateUseCase;
    }

    @PostMapping("/translate")
    public ResponseEntity<TranslateResponse> translate(@Valid @RequestBody TranslateRequest req) {
        log.atInfo().log("POST /api/v1/translate: text_len={}, target={}",
                req.text().length(), req.target_language());

        TranslationRequestDomain domainRequest = new TranslationRequestDomain(
                req.text(),
                req.source_language(),
                req.target_language()
        );

        TranslationResponseDomain result = translateUseCase.translate(domainRequest);

        return ResponseEntity.ok(new TranslateResponse(
                result.translatedText(),
                result.sourceLanguage(),
                result.targetLanguage(),
                result.cached()
        ));
    }
}
