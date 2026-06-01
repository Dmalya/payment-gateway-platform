package com.paymentgateway.auditledger.domain;

import java.math.BigDecimal;

public record PaymentEvent(
        String userId,
        BigDecimal amount,
        String currency,
        String merchantId,
        Double riskScore,
        String decision,
        String xTraceId
) {}