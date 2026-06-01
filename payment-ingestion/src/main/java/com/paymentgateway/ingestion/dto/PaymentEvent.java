package com.paymentgateway.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String traceId;
    private String timestamp;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
}