package com.paymentgateway.ingestion.config;

import com.paymentgateway.ingestion.dto.PaymentEvent;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

@Configuration
public class ReactorKafkaConfig {

    @Bean
    public KafkaSender<String, PaymentEvent> kafkaSender(KafkaProperties kafkaProperties) {
        // Build properties from application.yml
        Map<String, Object> producerProps = kafkaProperties.buildProducerProperties(null);

        SenderOptions<String, PaymentEvent> senderOptions = SenderOptions.create(producerProps);

        // You can tweak maxInFlight and other reactive properties here if needed
        senderOptions = senderOptions.maxInFlight(1024);

        return KafkaSender.create(senderOptions);
    }
}