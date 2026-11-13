package com.teamfourpixels.deepreaderbackend.port.out;

import com.teamfourpixels.deepreaderbackend.domain.model.TranslationRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.TranslationResponseDomain;

public interface TranslationPort {

    TranslationResponseDomain translate(TranslationRequestDomain request);
}
