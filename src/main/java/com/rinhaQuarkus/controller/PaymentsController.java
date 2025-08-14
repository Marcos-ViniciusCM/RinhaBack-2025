package com.rinhaQuarkus.controller;

import com.rinhaQuarkus.cache.CacheController;
import com.rinhaQuarkus.jdbc.api.DataService;
import com.rinhaQuarkus.model.PaymentRequest;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentsController {

    @Inject
    CacheController cache;

    @Inject
    DataService service;

  ExecutorService executor = Executors.newFixedThreadPool(20);
    //@RunOnVirtualThread
    @POST
    @Path("/payments")
    public Response createPayment(PaymentRequest pay){

        //cache.decideWich(pay);
     //  CompletableFuture.runAsync(() -> {
      // pay.setProcessor(Processor.FALLBACK);
     //  cache.decideWich(pay);
  //  });

      executor.submit(() -> {
        cache.decideWich(pay);
    });
    
        //cache.decideWich(pay);
        return Response.accepted().build();
      // return Response.ok().build();
    }



    @RunOnVirtualThread
    @GET
    @Path("/payments-summary")
    @Produces("application/json")
    public Response getPaymentSumary(@QueryParam(value = "from")Instant from , @QueryParam(value = "to") Instant to){
       try{
         long start = System.currentTimeMillis();
        Instant now = Instant.now();
       // PaymentsSumaryDto sumary = service.pegarPayments(from,to);
       long duration = System.currentTimeMillis() - start;
            
        
       return Response.ok(service.pegarPayments(from,to)).build();
         //System.out.println("Requisição Get demorou: " + duration + "ms");
       // return Response.ok("From: " + from.toString() + " To: " + to.toString()).build();
       }catch(Exception e){
           System.out.println("erro gerar reusmo");
        throw new WebApplicationException("Erro interno ao gerar o resumo", 500);

       }

    }


    @RunOnVirtualThread
    @GET
    @Path("/Therads")
    public Response Threads() {
        return Response.ok(Thread.currentThread()).build();
    }

}
