package com.paymentgateway.risk.model;

import java.math.BigDecimal;

public record IncomingPaymentEvent(
        String traceId,
        String timestamp,
        String userId,
        BigDecimal amount,
        String currency,
        String merchantId
) {}