package com.paymentgateway.ingestion.controller;

import com.paymentgateway.ingestion.dto.PaymentRequest;
import com.paymentgateway.ingestion.service.PaymentIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentIngestionService ingestionService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED) // Returns 202 immediately upon pipeline completion
    public Mono<Void> ingestPaymentEvent(
            @RequestHeader(value = "traceparent", required = true) String traceparent,
            @RequestBody PaymentRequest request) {

        // Extract the 32-char trace ID from the standard W3C header (00-{traceId}-{spanId}-01)
        String traceId = traceparent.length() >= 35 ? traceparent.split("-")[1] : traceparent;

        return ingestionService.ingestPayment(request, traceId);
    }
}