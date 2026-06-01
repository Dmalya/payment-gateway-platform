package com.paymentgateway.auditledger.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payment_ledger")
public record PaymentRecord(
        @Id
        String id,
        String userId,
        BigDecimal amount,
        String currency,
        String merchantId,
        Double riskScore,
        String decision,
        String xTraceId,
        @CreatedDate
        Instant createdAt
) {
    // Custom constructor to handle creation from a Kafka DTO without setting an ID
    public static PaymentRecord fromEvent(PaymentEvent event) {
        return new PaymentRecord(
                null, // Let MongoDB auto-generate the ID
                event.userId(),
                event.amount(),
                event.currency(),
                event.merchantId(),
                event.riskScore(),
                event.decision(),
                event.xTraceId(),
                null  // @CreatedDate will populate this upon save
        );
    }
}