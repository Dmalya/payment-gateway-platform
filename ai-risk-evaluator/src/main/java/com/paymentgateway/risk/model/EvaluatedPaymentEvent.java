package com.paymentgateway.risk.model;

public record EvaluatedPaymentEvent(
        IncomingPaymentEvent originalPayment,
        AiRiskEvaluation riskEvaluation
) {}