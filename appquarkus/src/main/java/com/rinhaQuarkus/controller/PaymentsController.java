package com.rinhaQuarkus.controller;

import com.rinhaQuarkus.cache.CacheController;
import com.rinhaQuarkus.model.PaymentRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentsController {

    @Inject
    CacheController cache;

    @POST
    public Response createPayment(PaymentRequest pay){
        cache.decideWich(pay);
        return Response.ok().build();
    }
}
