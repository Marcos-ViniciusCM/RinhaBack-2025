package com.rinhaQuarkus.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinhaQuarkus.DTO.PostPaymentDto;
import com.rinhaQuarkus.DTO.ServiceHealthDto;
import com.rinhaQuarkus.enums.Processor;
import com.rinhaQuarkus.jdbc.api.DataRepository;
import com.rinhaQuarkus.jdbc.api.DataService;
import com.rinhaQuarkus.model.PaymentRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.config.inject.ConfigProperty;




@ApplicationScoped
public class CacheController {

  


    private static final HttpClient client = HttpClient.newBuilder()
    .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    Queue<PaymentRequest> queue = new ConcurrentLinkedQueue<>();

    @Inject
    @ConfigProperty(name = "payment.processor.default.url")
    String paymentProcessorDefaultUrl;

    @Inject
    @ConfigProperty(name = "payment.processor.fallback.url")
    String paymentProcessorFallbackUrl;

    @Inject
    DataService service;

    @Inject
    DataRepository repository;

  
    //private final Semaphore semaphore = new Semaphore(20);





    public boolean decideWich(PaymentRequest pay){
        pay.setRequest_at(Instant.now());
        long start = System.currentTimeMillis();
        boolean primaryResult = doPostPaymentsDefaultTeste(pay,false);
        if(primaryResult){
            pay.setProcessor(Processor.DEFAULT);
            repository.save(pay);
            return true;
        }
        boolean fallbackResult = doPostPaymentsDefaultTeste(pay,true);
        if(fallbackResult){
            pay.setProcessor(Processor.FALLBACK);
            repository.save(pay);
            return  true;
        }else {
            System.out.println("Nao foi salvo em nenhum");
            return false;
        }

    }
   

//    public void decideWich(PaymentRequest pay){
//        long start = System.currentTimeMillis();
//        boolean sucess;
//        try{
//            semaphore.acquire();
//            Instant now = Instant.now();
//            pay.setRequest_at(now);
//
//            sucess = doPostPaymentsDefaultTeste(pay, false);
//            long duration = System.currentTimeMillis() - start;
//            if(sucess){
//                pay.setProcessor(Processor.DEFAULT);
//                service.inserirPayment(pay);
//            }
//            System.out.println("Requisição levou Default: " + duration + "ms");
//        }catch(Exception e1){
//            System.out.println("Estamos no catch aeeeeee: ");
//
//            sucess =doPostPaymentsDefaultTeste(pay, true);
//             if(sucess){
//                 pay.setProcessor(Processor.FALLBACK);
//                 service.inserirPayment(pay);
//             }
//            long duration = System.currentTimeMillis() - start;
//            System.out.println("Fizemos um fallback: "+ duration +"ms");
//
//        }finally{
//            long duration2 = System.currentTimeMillis() - start;
//            System.out.println("Liberou a thread em: " + duration2 + "ms");
//            semaphore.release();
//        }
//
//
//
//    }


   


     public  boolean doPostPaymentsDefaultTeste(PaymentRequest pay , boolean fallback){
         long start = System.currentTimeMillis();
         String url = fallback ? paymentProcessorFallbackUrl : paymentProcessorDefaultUrl;  

            try {
                    String requestedAtFormatted = pay.getRequest_at()
                        .atOffset(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                    Map<String, Object> payload = Map.of(
                        "correlationId", pay.getCorrelationId().toString(),
                        "amount", pay.getAmount(),
                        "requestedAt", requestedAtFormatted
                );
                String jsonPayload = objectMapper.writeValueAsString(payload);
                System.out.println(" PaymentRequest em JSON: default " + jsonPayload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(800))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .build();

                     HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    int status = response.statusCode();
                    if(status >=200 & status <=299){
            long duration = System.currentTimeMillis() - start;
            System.out.println("Do Post Teste demorou: " + duration + "ms");
            System.out.println("status body: " +status);

                       return true;
                        
                    }
            } catch (Exception e) { 
                //return false;
              throw new RuntimeException("Erro na chamada HTTP do post", e);
            }  
       return false;
    }



    public CompletableFuture<Boolean> doPostPaymentsDefault(PaymentRequest pay , boolean fallback){

        String url = fallback ? paymentProcessorFallbackUrl : paymentProcessorDefaultUrl;
            try {
                //PostPaymentDto dto = new PostPaymentDto(pay.getCorrelationId().toString(),pay.getAmount(),pay.getRequest_at().toString());
                String requestedAtFormatted = pay.getRequest_at()
    .atOffset(ZoneOffset.UTC)
    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                  Map<String, Object> payload = Map.of(
            "correlationId", pay.getCorrelationId().toString(),
            "amount", pay.getAmount(),
            "requestedAt", requestedAtFormatted
        );
                String jsonPayload = objectMapper.writeValueAsString(payload);
                //System.out.println(" PaymentRequest em JSON: default " + jsonPayload);




            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .build();



                return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(response -> {
                            if(response.statusCode() >= 200 && response.statusCode() <= 299){
                             
                              return true;
                            }
                            return false;
                        })
                        .exceptionally(ex ->{
                            return false;
                        });



            } catch (Exception e) {
                CompletableFuture<Boolean> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(e);
                return failedFuture;
              // throw new RuntimeException("Erro na chamada HTTP do post", e);
            }
           
    }

    


}

