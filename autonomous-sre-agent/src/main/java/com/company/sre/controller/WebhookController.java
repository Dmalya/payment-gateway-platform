package com.company.sre.controller;

import com.company.sre.service.SreAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final SreAgentService sreAgentService;

    public WebhookController(SreAgentService sreAgentService) {
        this.sreAgentService = sreAgentService;
    }

    @PostMapping("/alertmanager")
    public ResponseEntity<String> handleAlertmanagerWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Incoming Prometheus Alertmanager webhook triggered. Acknowledging immediately.");

        // Process asynchronously to prevent Alertmanager timeout loops
        CompletableFuture.runAsync(() -> {
            try {
                String agentResponse = sreAgentService.handleAlert(payload);
                log.info("SRE Agent Action Summary:\n{}", agentResponse);
            } catch (Exception e) {
                log.error("Failed to process alert through SRE Agent", e);
            }
        });

        // Instantly satisfy the Prometheus webhook requirement
        return ResponseEntity.accepted().body("Alert received. SRE Agent is investigating.");
    }
}