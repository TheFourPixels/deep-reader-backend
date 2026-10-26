package com.teamfourpixels.deepreaderbackend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MetricsForwardController {

    @GetMapping("/metrics")
    public String forwardMetrics() {
        return "forward:/actuator/prometheus";
    }
}
