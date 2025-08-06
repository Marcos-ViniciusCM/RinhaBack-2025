package com.rinhaQuarkus.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.rinhaQuarkus.DTO.PaymentsSumaryDto;
import com.rinhaQuarkus.cache.CacheController;
import com.rinhaQuarkus.enums.Processor;
import com.rinhaQuarkus.jdbc.api.DataService;
import com.rinhaQuarkus.model.PaymentRequest;

import io.quarkus.vertx.http.runtime.devmode.Json.JsonObjectBuilder;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;



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
        System.out.println("olll");
        //cache.decideWich(pay);
     //  CompletableFuture.runAsync(() -> {
      // pay.setProcessor(Processor.FALLBACK);
     //  cache.decideWich(pay);
  //  });

    //   Thread.startVirtualThread(() ->
   //    {
     //   cache.decideWich(pay);
     //  });
        cache.decideWich(pay);
        //return Response.accepted().build();
       return Response.ok("post1").build();
    }


    @POST
    @Path("/te")
    public Response teste(PaymentRequest pay){
        System.out.println("olll");
       return Response.ok("post2").build();
    }

    @GET
    @Path("/payments-summary")
    public Response getPaymentSumary(@QueryParam("from")Instant from , @QueryParam("to") Instant to){
       try{

        PaymentsSumaryDto sumary = service.pegarPayments(from,to);
        return Response.ok(sumary).build();
       }catch(Exception e){

        throw new WebApplicationException("Erro interno ao gerar o resumo", 500);
       }

    }

    @GET
    @Path("/payments-tt")
    public Response getPaymentSumary(){
        return Response.ok("oiiiiissss23").build();
    }
}
