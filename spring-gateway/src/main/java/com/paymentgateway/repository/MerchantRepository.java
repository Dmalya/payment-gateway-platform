package com.paymentgateway.repository;

import com.paymentgateway.domain.Merchant;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MerchantRepository extends ReactiveCrudRepository<Merchant, String> {
    Mono<Merchant> findByMerchantId(String merchantId);
}