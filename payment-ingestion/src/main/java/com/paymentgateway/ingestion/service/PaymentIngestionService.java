package com.paymentgateway.ingestion.service;

import com.paymentgateway.ingestion.dto.PaymentEvent;
import com.paymentgateway.ingestion.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentIngestionService {

    private final KafkaSender<String, PaymentEvent> kafkaSender;

    @Value("${payment.kafka.topic}")
    private String topic;

    public Mono<Void> ingestPayment(PaymentRequest request, String traceId) {
        PaymentEvent event = new PaymentEvent(
                traceId,
                Instant.now().toString(),
                request.userId(),      // Updated from getUserId()
                request.amount(),      // Updated from getAmount()
                request.currency(),    // Updated from getCurrency()
                request.merchantId()   // Updated from getMerchantId()
        );

        SenderRecord<String, PaymentEvent, String> record = SenderRecord.create(
                new ProducerRecord<>(topic, event.userId(), event), // Updated from getUserId()
                traceId
        );

        return kafkaSender.send(Mono.just(record))
                .doOnError(e -> log.error("Error sending payment event to Kafka. TraceId: {}", traceId, e))
                .then();
    }
}