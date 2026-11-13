package com.teamfourpixels.deepreaderbackend.port.in;

import com.teamfourpixels.deepreaderbackend.domain.model.TranslationRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;

public interface TranslateUseCase {
    TranslationResponseDomain translate(TranslationRequestDomain request);
}
