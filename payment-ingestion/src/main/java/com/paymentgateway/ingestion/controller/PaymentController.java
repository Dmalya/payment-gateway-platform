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
            @RequestHeader(value = "X-Trace-Id", required = true) String traceId,
            @RequestBody PaymentRequest request) {

        return ingestionService.ingestPayment(request, traceId);
    }
}