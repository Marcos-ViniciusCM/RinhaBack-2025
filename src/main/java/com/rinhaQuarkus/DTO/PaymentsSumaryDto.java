package com.rinhaQuarkus.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentsSumaryDto(@JsonProperty("default") DefaultSumaryDto _default, FallbackSumaryDto fallback ) {
}
