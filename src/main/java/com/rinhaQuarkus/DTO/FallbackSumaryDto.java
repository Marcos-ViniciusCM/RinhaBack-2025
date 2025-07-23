package com.rinhaQuarkus.DTO;

import java.math.BigDecimal;

public record FallbackSumaryDto(int totalRequest, BigDecimal totalAmount) {
}
