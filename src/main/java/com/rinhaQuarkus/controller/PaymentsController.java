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

        //cache.decideWich(pay);
     //  CompletableFuture.runAsync(() -> {
      // pay.setProcessor(Processor.FALLBACK);
     //  cache.decideWich(pay);
  //  });

       Thread.startVirtualThread(() ->
       {
        cache.decideWich(pay);
       });
        //cache.decideWich(pay);
        //return Response.accepted().build();
       return Response.ok("post1").build();
    }




    @GET
    @Path("/payments-summary")
    @Produces("application/json")
    public Response getPaymentSumary(@QueryParam(value = "from")Instant from , @QueryParam(value = "to") Instant to){
       try{

       // PaymentsSumaryDto sumary = service.pegarPayments(from,to);
        return Response.ok(service.pegarPayments(from,to)).build();
          // return Response.ok("From: " + from.toString() + " To: " + to.toString()).build();
       }catch(Exception e){
           System.out.println("erro gerar reusmo");
        throw new WebApplicationException("Erro interno ao gerar o resumo", 500);

       }

    }

}
