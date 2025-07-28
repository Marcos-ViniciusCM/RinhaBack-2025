package com.rinhaQuarkus.controller;

import com.rinhaQuarkus.DTO.PaymentsSumaryDto;
import com.rinhaQuarkus.cache.CacheController;
import com.rinhaQuarkus.jdbc.api.DataService;
import com.rinhaQuarkus.model.PaymentRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentsController {

    @Inject
    CacheController cache;

    @Inject
    DataService service;

    @POST
    @Path("/payments")
    public Response createPayment(PaymentRequest pay){
        //cache.decideWich(pay);
        CompletableFuture.runAsync(() -> cache.decideWich(pay));
        return Response.accepted().build();
       // return Response.ok().build();
    }

    @GET
    @Path("/payments-summary")
    public Response getPaymentSumary(@QueryParam("from")Instant from , @QueryParam("to") Instant to){
        PaymentsSumaryDto sumary = service.pegarPayments(from,to);
        return Response.ok(sumary).build();
    }

    @GET
    @Path("/payments-tt")
    public Response getPaymentSumary(){
        return Response.ok("oiiiiissss2").build();
    }
}
