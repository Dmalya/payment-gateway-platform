package com.paymentgateway.auditledger.domain;

import java.math.BigDecimal;

public record PaymentEvent(
        OriginalPayment originalPayment,
        RiskEvaluation riskEvaluation
) {
    public record OriginalPayment(
            String traceId,
            String timestamp,
            String userId,
            BigDecimal amount,
            String currency,
            String merchantId
    ) {}

    public record RiskEvaluation(
            int riskScore,
            String decision
    ) {}
}