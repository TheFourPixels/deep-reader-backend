package com.teamfourpixels.deepreaderbackend.port.in;

import com.teamfourpixels.deepreaderbackend.domain.model.ExplainRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainResponseDomain;

public interface ExplainUseCase {
    ExplainResponseDomain explain(ExplainRequestDomain request);
}
