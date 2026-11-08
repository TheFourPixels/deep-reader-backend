package com.teamfourpixels.deepreaderbackend;

import com.teamfourpixels.deepreaderbackend.config.ChaosInterceptor;
import com.teamfourpixels.deepreaderbackend.controller.ChaosController;
import com.teamfourpixels.deepreaderbackend.exception.ChaosException;
import com.teamfourpixels.deepreaderbackend.service.ChaosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChaosServiceTest {

    private ChaosService chaosService;
    private ChaosController chaosController;
    private ChaosInterceptor chaosInterceptor;

    @BeforeEach
    void setUp() {
        chaosService = new ChaosService();
        chaosController = new ChaosController(chaosService);
        chaosInterceptor = new ChaosInterceptor(chaosService);
    }

    @Test
    void testInitialStateIsNormal() {
        Map<String, Object> status = chaosService.getStatus();
        assertFalse((Boolean) status.get("enabled"));
        assertEquals(0.0, (Double) status.get("errorRate"));
        assertEquals(0, (Integer) status.get("delayMs"));

        assertDoesNotThrow(() -> chaosService.applyChaosIfNeeded());
    }

    @Test
    void testTriggerFaultAndReset() {
        chaosController.triggerFault(0.5, 200);
        Map<String, Object> status = chaosService.getStatus();
        assertTrue((Boolean) status.get("enabled"));
        assertEquals(0.5, (Double) status.get("errorRate"));
        assertEquals(200, (Integer) status.get("delayMs"));

        chaosController.reset();
        Map<String, Object> resetStatus = chaosService.getStatus();
        assertFalse((Boolean) resetStatus.get("enabled"));
        assertEquals(0.0, (Double) resetStatus.get("errorRate"));
        assertEquals(0, (Integer) resetStatus.get("delayMs"));
    }

    @Test
    void testChaosExceptionThrownWhenErrorRateIsOne() {
        chaosService.triggerFault(1.0, 0);
        assertThrows(ChaosException.class, () -> chaosService.applyChaosIfNeeded());
    }

    @Test
    void testChaosInterceptorExcludesHealthAndChaosEndpoints() {
        chaosService.triggerFault(1.0, 0);

        MockHttpServletRequest healthRequest = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(chaosInterceptor.preHandle(healthRequest, response, new Object()));

        MockHttpServletRequest chaosRequest = new MockHttpServletRequest("GET", "/api/chaos/status");
        assertTrue(chaosInterceptor.preHandle(chaosRequest, response, new Object()));

        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/books");
        assertThrows(ChaosException.class, () -> chaosInterceptor.preHandle(apiRequest, response, new Object()));
    }
}
