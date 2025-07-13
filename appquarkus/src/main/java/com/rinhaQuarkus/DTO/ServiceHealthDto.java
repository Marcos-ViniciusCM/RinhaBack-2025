package com.rinhaQuarkus.DTO;

import java.sql.Timestamp;

public record ServiceHealthDto(boolean failling , int minResponseTime ) {
}
