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




    //@RunOnVirtualThread
    @POST
    @Path("/payments")
    public Response createPayment(PaymentRequest pay){

     
        boolean sucess = cache.decideWich(pay);

        if(sucess){
            return Response.accepted().build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Payment processing failed")
                .build();


    }



    //@RunOnVirtualThread
    @GET
    @Path("/payments-summary")
    @Produces("application/json")
    public Response getPaymentSumary(@QueryParam(value = "from")Instant from , @QueryParam(value = "to") Instant to){
       try{
         long start = System.currentTimeMillis();
        Instant now = Instant.now();
       // PaymentsSumaryDto sumary = service.pegarPayments(from,to);
       long duration = System.currentTimeMillis() - start;

           //System.out.println("Requisição JSON: " + repository.sumAmount(from,to));
       return Response.ok(service.pegarPayments(from, to)).build();
        
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
