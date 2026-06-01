package com.paymentgateway.risk.model;

public record AiRiskEvaluation(
        int riskScore,
        String decision
) {}