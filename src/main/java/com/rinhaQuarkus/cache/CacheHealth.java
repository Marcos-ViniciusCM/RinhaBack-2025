package com.rinhaQuarkus.cache;

import com.rinhaQuarkus.DTO.ServiceHealthDto;

import java.time.Instant;

public class CacheHealth {

    private ServiceHealthDto data;
    private Instant expiredAt;

    public CacheHealth(ServiceHealthDto data, Instant expiredAt) {
        this.data = data;
        this.expiredAt = expiredAt;
    }

    public CacheHealth() {
    }


   public boolean isExpired(){
        return Instant.now().isAfter(expiredAt);
   }

   public ServiceHealthDto getData(){
        return data;
   }


}
