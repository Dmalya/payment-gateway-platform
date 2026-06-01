package com.paymentgateway.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.ChatClient;

@SpringBootApplication
public class AiRiskEvaluatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRiskEvaluatorApplication.class, args);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}