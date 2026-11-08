package com.teamfourpixels.deepreaderbackend.config;

import com.teamfourpixels.deepreaderbackend.service.ChaosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ChaosInterceptor implements HandlerInterceptor {

    private final ChaosService chaosService;

    public ChaosInterceptor(ChaosService chaosService) {
        this.chaosService = chaosService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/chaos") || uri.startsWith("/actuator") || uri.equals("/metrics") || uri.equals("/api/health")) {
            return true;
        }

        chaosService.applyChaosIfNeeded();
        return true;
    }
}
