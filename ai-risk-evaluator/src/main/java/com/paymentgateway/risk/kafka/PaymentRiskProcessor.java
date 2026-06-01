package com.paymentgateway.risk.kafka;

import com.paymentgateway.risk.model.AiRiskEvaluation;
import com.paymentgateway.risk.model.EvaluatedPaymentEvent;
import com.paymentgateway.risk.model.IncomingPaymentEvent;
import com.paymentgateway.risk.service.AiRiskEvaluatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentRiskProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentRiskProcessor.class);
    private static final String OUTPUT_TOPIC = "risk-scored-payments";

    private final AiRiskEvaluatorService aiRiskEvaluatorService;
    private final KafkaTemplate<String, EvaluatedPaymentEvent> kafkaTemplate;

    public PaymentRiskProcessor(AiRiskEvaluatorService aiRiskEvaluatorService,
                                KafkaTemplate<String, EvaluatedPaymentEvent> kafkaTemplate) {
        this.aiRiskEvaluatorService = aiRiskEvaluatorService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "incoming-payments", groupId = "risk-evaluator-group")
    public void processIncomingPayment(IncomingPaymentEvent paymentEvent) {
        try {
            log.info("Received payment event via Kafka: {}", paymentEvent.traceId());

            // 1. Ask the AI to evaluate
            AiRiskEvaluation evaluation = aiRiskEvaluatorService.evaluatePayment(paymentEvent);
            log.info("Evaluation complete for {}: Score={}, Decision={}",
                    paymentEvent.traceId(), evaluation.riskScore(), evaluation.decision());

            // 2. Combine into a new event
            EvaluatedPaymentEvent evaluatedEvent = new EvaluatedPaymentEvent(paymentEvent, evaluation);

            // 3. Publish to Downstream Topic
            // Using the traceId as the Kafka Partition Key ensures chronological ordering per request
            kafkaTemplate.send(OUTPUT_TOPIC, paymentEvent.traceId(), evaluatedEvent);

        } catch (Exception e) {
            log.error("Failed to process payment risk evaluation for TraceId: {}", paymentEvent.traceId(), e);
            // In a strict production environment, route the failed payload to a Dead Letter Queue (DLQ) here
        }
    }
}