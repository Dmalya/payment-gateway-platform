package com.paymentgateway.auditledger.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payment_ledger")
public record PaymentRecord(
        @Id String id,
        String userId,
        BigDecimal amount,
        String currency,
        String merchantId,
        Integer riskScore,
        String decision,
        String traceId,
        @CreatedDate Instant createdAt
) {
    public static PaymentRecord fromEvent(PaymentEvent event) {
        return new PaymentRecord(
                null,
                event.originalPayment().userId(),
                event.originalPayment().amount(),
                event.originalPayment().currency(),
                event.originalPayment().merchantId(),
                event.riskEvaluation().riskScore(),
                event.riskEvaluation().decision(),
                event.originalPayment().traceId(),
                null
        );
    }
}