package com.rinhaQuarkus.model;


import com.rinhaQuarkus.enums.Processor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentRequest {

    private UUID id;

    private BigDecimal amount;

    private Processor processor;

    private Instant request_at;


    public PaymentRequest(UUID id, BigDecimal amount, Processor processor, Instant request_at) {
        this.id = id;
        this.amount = amount;
        this.processor = processor;
        this.request_at = request_at;
    }

    public PaymentRequest() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}
