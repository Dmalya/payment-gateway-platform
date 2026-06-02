package com.paymentgateway.ingestion.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        String userId,
        BigDecimal amount,
        String currency,
        String merchantId
) {}