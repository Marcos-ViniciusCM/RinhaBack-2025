package com.rinhaQuarkus.model;


import com.rinhaQuarkus.enums.Processor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table  (name = "payments")
public class PaymentRequest {
    @Id
    private UUID correlationId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    private Processor processor;

    @Column(name = "requested_at", nullable = false)
    private Instant request_at;


    public PaymentRequest(UUID correlationId, BigDecimal amount, Processor processor, Instant request_at) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.processor = processor;
        this.request_at = request_at;
    }

    public PaymentRequest() {
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    public Instant getRequest_at() {
        return request_at;
    }

    public void setRequest_at(Instant request_at) {
        this.request_at = request_at;
    }

    public void decideProcessor(String processor){
            if(processor.equals("fallback")){
                this.processor = Processor.FALLBACK;
            }else{
                this.processor = Processor.DEFAULT;
            }
    }
}
