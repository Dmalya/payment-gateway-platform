package com.paymentgateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymentgateway.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final MerchantRepository merchantRepository;

    @Bean
    public RouteLocator paymentRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("payment_ingest_route", r -> r.path("/ingest")
                        .and().method(HttpMethod.POST)
                        .filters(f -> f
                                // 1. Log and trace
                                .filter((exchange, chain) -> {
                                    String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
                                    log.info("Incoming payment request. X-Trace-Id: {}", traceId != null ? traceId : "MISSING");
                                    return chain.filter(exchange);
                                })
                                .rewritePath("/ingest", "/api/v1/events")  // ← add this
                                // 2. Extract payload, validate merchant, forward payload
                                .modifyRequestBody(JsonNode.class, JsonNode.class, (exchange, jsonNode) -> {
                                    if (jsonNode == null || !jsonNode.has("merchantId")) {
                                        log.warn("Invalid payload: Missing merchantId");
                                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing merchantId"));
                                    }

                                    String merchantId = jsonNode.get("merchantId").asText();

                                    return merchantRepository.findByMerchantId(merchantId)
                                            .filter(merchant -> merchant.isActive())
                                            .switchIfEmpty(Mono.defer(() -> {
                                                log.warn("Merchant validation failed for merchantId: {}", merchantId);
                                                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or inactive merchant"));
                                            }))
                                            .doOnNext(merchant -> log.info("Merchant {} successfully validated.", merchantId))
                                            .thenReturn(jsonNode); // Return original payload to pass downstream
                                })

                        )
                        .uri("http://app-payment-ingestion:8082")
                )
                .build();
    }
}