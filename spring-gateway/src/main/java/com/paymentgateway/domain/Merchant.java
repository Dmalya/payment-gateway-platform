package com.paymentgateway.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("merchants")
public record Merchant(
        @Id String merchantId,
        boolean isActive
) {}