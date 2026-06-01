package com.paymentgateway.auditledger.consumer;

import com.paymentgateway.auditledger.domain.PaymentEvent;
import com.paymentgateway.auditledger.domain.PaymentRecord;
import com.paymentgateway.auditledger.repository.PaymentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private final PaymentRecordRepository repository;

    public PaymentEventConsumer(PaymentRecordRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "risk-scored-payments")
    public void consume(PaymentEvent event) {
        log.info("Received risk-scored payment event. [X-Trace-Id: {}]", event.xTraceId());

        try {
            PaymentRecord record = PaymentRecord.fromEvent(event);
            PaymentRecord savedRecord = repository.save(record);

            log.info("Successfully persisted payment record to ledger. [Record ID: {}]", savedRecord.id());
        } catch (Exception e) {
            log.error("Failed to persist payment event to audit ledger. [X-Trace-Id: {}]", event.xTraceId(), e);
            // In a production scenario, you would likely route to a Dead Letter Queue (DLQ) here.
            throw e;
        }
    }
}