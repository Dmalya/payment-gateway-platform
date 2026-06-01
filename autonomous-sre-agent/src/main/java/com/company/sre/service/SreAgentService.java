package com.company.sre.service;

import com.company.sre.tools.SreTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SreAgentService {

    private static final Logger log = LoggerFactory.getLogger(SreAgentService.class);
    private final ChatClient chatClient;
    private final SreTools sreTools;

    // Inject SreTools alongside the ChatClient Builder
    public SreAgentService(ChatClient.Builder chatClientBuilder, SreTools sreTools) {
        this.sreTools = sreTools;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                You are a Senior Level Autonomous Site Reliability Engineer (SRE).
                Your job is to analyze incoming infrastructure and application alerts,
                diagnose the root cause, and take automated remediation steps using the tools provided.
                
                Always investigate the logs first if a service is mentioned.
                If the service appears completely hung or deadlocked based on your log investigation, 
                restart the pod. Document your thought process and the actions you took.
                """)
                .build();
    }

    public String handleAlert(Map<String, Object> alertPayload) {
        log.info("Received alert payload for processing. Engaging SRE Agent.");

        return this.chatClient.prompt()
                .user(alertPayload.toString())
                .tools(this.sreTools) // Pass the actual object instance instead of the string "sreTools"
                .call()
                .content();
    }
}