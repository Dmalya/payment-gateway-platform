package com.paymentgateway.auditledger.repository;

import com.paymentgateway.auditledger.domain.PaymentRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRecordRepository extends MongoRepository<PaymentRecord, String> {
}