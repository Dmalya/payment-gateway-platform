package com.paymentgateway.risk.service;

import com.paymentgateway.risk.model.AiRiskEvaluation;
import com.paymentgateway.risk.model.IncomingPaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiRiskEvaluatorService {

    private static final Logger log = LoggerFactory.getLogger(AiRiskEvaluatorService.class);
    private final ChatClient chatClient;

    public AiRiskEvaluatorService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AiRiskEvaluation evaluatePayment(IncomingPaymentEvent event) {
        log.info("Evaluating payment risk for TraceId: {}", event.traceId());

        var outputConverter = new BeanOutputConverter<>(AiRiskEvaluation.class);
        String formatInstructions = outputConverter.getFormat();

        String promptTemplate = """
            You are a real-time payment fraud detection system. 
            Evaluate the following transaction for anomalies.
            Look for highly suspicious patterns like unusual amounts, or generic high-risk indicators.
            
            Transaction Details:
            - User ID: {userId}
            - Amount: {amount}
            - Currency: {currency}
            - Merchant ID: {merchantId}
            
            {formatInstructions}
            """;

        String response = this.chatClient.prompt()
                .user(u -> u.text(promptTemplate)
                        .param("userId", event.userId())
                        .param("amount", event.amount().toString())
                        .param("currency", event.currency())
                        .param("merchantId", event.merchantId())
                        .param("formatInstructions", formatInstructions))
                .call()
                .content();

        return outputConverter.convert(response);
    }
}