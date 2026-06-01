package com.paymentgateway.risk.model;

import java.math.BigDecimal;

public record IncomingPaymentEvent(
        private String traceId;
        private String timestamp;
        private String userId;
        private BigDecimal amount;
        private String currency;
        private String merchantId;
) {}