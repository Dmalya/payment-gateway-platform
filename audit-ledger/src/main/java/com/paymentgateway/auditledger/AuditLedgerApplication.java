package com.paymentgateway.auditledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class AuditLedgerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditLedgerApplication.class, args);
    }
}