package com.rinhaQuarkus.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PostPaymentDto(@JsonProperty("correlationId")String correlationId,
                             @JsonProperty("amount") BigDecimal amount,
                             @JsonProperty("requestedAt") String request_at) {
}
