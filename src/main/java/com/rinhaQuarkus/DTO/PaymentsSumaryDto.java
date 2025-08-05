package com.rinhaQuarkus.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentsSumaryDto(@JsonProperty("default") DefaultSumaryDto _ddefault, @JsonProperty("fallback")FallbackSumaryDto fallback ) {


    @JsonProperty("default")
    public DefaultSumaryDto getDefault() {
        return _ddefault;
    }
}
