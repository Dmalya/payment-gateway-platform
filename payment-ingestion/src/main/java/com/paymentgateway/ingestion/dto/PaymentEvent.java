package com.paymentgateway.ingestion.dto;

import java.math.BigDecimal;

public record PaymentEvent(
        String traceId,
        String timestamp,
        String userId,
        BigDecimal amount,
        String currency,
        String merchantId
) {}