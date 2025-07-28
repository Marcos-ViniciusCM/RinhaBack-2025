package com.rinhaQuarkus.DTO;

import java.math.BigDecimal;

public record FallbackSumaryDto(int totalRequests, BigDecimal totalAmount) {
}
