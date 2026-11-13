package com.teamfourpixels.deepreaderbackend.adapter.in.web;

import com.teamfourpixels.deepreaderbackend.adapter.in.web.dto.ExplainRequest;
import com.teamfourpixels.deepreaderbackend.adapter.in.web.dto.ExplainResponse;
import com.teamfourpixels.deepreaderbackend.domain.model.*;
import com.teamfourpixels.deepreaderbackend.port.in.ExplainUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ExplainController {

    private static final Logger log = LoggerFactory.getLogger(ExplainController.class);

    private final ExplainUseCase explainUseCase;

    public ExplainController(ExplainUseCase explainUseCase) {
        this.explainUseCase = explainUseCase;
    }

    @PostMapping("/explain")
    public ResponseEntity<ExplainResponse> explain(@Valid @RequestBody ExplainRequest req) {
        log.atInfo().log("POST /api/v1/explain: book_id='{}', selected='{}'", req.book_id(), req.selected_text());

        ExplainRequestDomain domain = toDomain(req);
        ExplainResponseDomain result = explainUseCase.explain(domain);
        return ResponseEntity.ok(toResponse(result));
    }

    private ExplainRequestDomain toDomain(ExplainRequest req) {
        ExplainRequest.SurroundingContextDto ctxDto = req.surrounding_context();
        SurroundingContext ctx = new SurroundingContext(
                ctxDto.sentences_before() != null ? ctxDto.sentences_before() : List.of(),
                ctxDto.sentences_after()  != null ? ctxDto.sentences_after()  : List.of(),
                ctxDto.paragraph(),
                ctxDto.chapter_context()
        );
        return new ExplainRequestDomain(
                req.book_id(),
                req.selected_text(),
                req.current_sentence(),
                ctx,
                req.target_language() != null ? req.target_language() : "ru"
        );
    }

    private ExplainResponse toResponse(ExplainResponseDomain d) {
        return new ExplainResponse(
                d.selectedText(),
                d.contextualMeaning(),
                d.phenomenonType().name().toLowerCase(),
                d.explanation(),
                d.stylisticColoring().name().toLowerCase(),
                d.spoilerAlert(),
                d.cached(),
                d.processingTimeMs()
        );
    }
}
