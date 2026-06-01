package com.paymentgateway.risk.model;

import java.math.BigDecimal;

public record IncomingPaymentEvent(
        String userId,
        BigDecimal amount,
        String currency,
        String merchantId,
        String traceId
) {}