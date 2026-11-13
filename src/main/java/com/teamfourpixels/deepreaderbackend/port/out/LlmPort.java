package com.teamfourpixels.deepreaderbackend.port.out;

import com.teamfourpixels.deepreaderbackend.domain.model.ExplainRequestDomain;
import com.teamfourpixels.deepreaderbackend.domain.model.ExplainResponseDomain;

public interface LlmPort {

    ExplainResponseDomain explain(ExplainRequestDomain request);
}
