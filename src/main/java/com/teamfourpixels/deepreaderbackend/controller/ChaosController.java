package com.teamfourpixels.deepreaderbackend.controller;

import com.teamfourpixels.deepreaderbackend.service.ChaosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chaos")
public class ChaosController {

    private final ChaosService chaosService;

    public ChaosController(ChaosService chaosService) {
        this.chaosService = chaosService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(chaosService.getStatus());
    }

    @PostMapping("/fault")
    public ResponseEntity<Map<String, Object>> triggerFault(
            @RequestParam(defaultValue = "0.4") double errorRate,
            @RequestParam(defaultValue = "500") int delayMs) {
        chaosService.triggerFault(errorRate, delayMs);
        return ResponseEntity.ok(chaosService.getStatus());
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        chaosService.reset();
        return ResponseEntity.ok(chaosService.getStatus());
    }
}
