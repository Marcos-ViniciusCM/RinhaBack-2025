package com.rinhaQuarkus.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinhaQuarkus.DTO.ServiceHealthDto;
import com.rinhaQuarkus.enums.Processor;
import com.rinhaQuarkus.jdbc.api.DataService;
import com.rinhaQuarkus.model.PaymentRequest;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;




@ApplicationScoped
public class CacheController {

    private Map<String , CacheHealth> cache = new ConcurrentHashMap<>();

    final Queue<PaymentRequest> payments = new ConcurrentLinkedQueue<>();

    private final Set<String> paymentsId = ConcurrentHashMap.newKeySet();

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    DataService service;

   // @Inject
   // WebClient webClient;



    private ServiceHealthDto checkCache(String key){
        CacheHealth cached = cache.get(key);
        if(cached != null && !cached.isExpired()){
            return cached.getData();
        }
        //ServiceHealthDto fresh = callHeathCheck(key);
        ServiceHealthDto fresh = callHeathCheck();
        cache.put(key, new CacheHealth(fresh , Instant.now().plusSeconds(5)));
        return fresh;
    }

    @Scheduled(every = "5s")
    public void updateCache(){
        try{
            ServiceHealthDto fresh = callHeathCheck();
            cache.put("default", new CacheHealth(fresh, Instant.now().plusSeconds(5)));

        }catch(Exception e){
            System.err.println("Erro ao atualizar cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ServiceHealthDto callHeathCheck(){
        try{
                String url = ( "http://payment-processor-default:8080/payments/service-health");

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() > 200 && response.statusCode()  < 299){
                    String json = response.body();
                    return objectMapper.readValue(json, ServiceHealthDto.class);
                }

                       
            }catch(IOException | InterruptedException e){
                throw new RuntimeException("Erro na chamada HTTP", e);
            }
        return new ServiceHealthDto(true,1000);
    }

    public void decideWich(PaymentRequest pay){
        //ServiceHealthDto health = checkCache("default");
        ServiceHealthDto health = cache.get("default").getData();

       
    if(health.failing()){
        Instant now = Instant.now();
        pay.setRequest_at(now);
        pay.setProcessor(Processor.FALLBACK);
        doPostPaymentsFallback(pay);
        //payIfNotExist(pay);
    }else{
        Instant now = Instant.now();
        pay.setRequest_at(now);
        pay.setProcessor(Processor.DEFAULT);
        doPostPaymentsDefault( pay);
       // payIfNotExist(pay);
    }

       
    
      
      //  pay.setProcessor(Processor.DEFAULT);
      //  Instant now = Instant.now();
      //  pay.setRequest_at(now);
      //  doPostPayments( pay, "default");
   

//        boolean sucess = doPostPayments(pay,processor);
//        if(sucess){
//            service.inserirPayment(pay);
//        }
    }


    public CompletableFuture<Void> doPostPaymentsDefault(PaymentRequest pay){

        String url = "http://payment-processor-default:8080/payments";
            try {
                    String jsonPayload = "{"
            + "\"correlationId\":\"" + pay.getCorrelationId() + "\","
            + "\"amount\":" + pay.getAmount() + ","
            + "\"requestedAt\":\"" + pay.getRequest_at() + "\""
            + "}";
             System.out.println(" PaymentRequest em JSON: " + jsonPayload);



            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .build();
//                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//                System.out.println("Status Code: " + response.statusCode());
//                System.out.println("Response Body: " + response.body());

                return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if(response.statusCode() >= 200 && response.statusCode() <= 299){
                               payIfNotExist(pay);
                            };
                        });

              
               
            } catch (Exception e) {
                CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(e);
                return failedFuture;
              // throw new RuntimeException("Erro na chamada HTTP do post", e);
            }
    }

    public CompletableFuture<Void> doPostPaymentsFallback(PaymentRequest pay){

        String url = "http://payment-processor-fallback:8080/payments";
        try {
            String jsonPayload = "{"
                    + "\"correlationId\":\"" + pay.getCorrelationId() + "\","
                    + "\"amount\":" + pay.getAmount() + ","
                    + "\"requestedAt\":\"" + pay.getRequest_at() + "\""
                    + "}";
            System.out.println(" PaymentRequest em JSON: " + jsonPayload);



            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .build();
          //  HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
           // System.out.println("Status Code: " + response.statusCode());
           // System.out.println("Response Body: " + response.body());
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response ->{
                        if(response.statusCode() >= 200 && response.statusCode() <= 299){
                            payIfNotExist(pay);
                        };
                    });



        } catch (Exception e) {
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
           // throw new RuntimeException("Erro na chamada HTTP do post fallbakc", e);

        }


    }


    private void doPostPaymentsArray(String payments){

            while(!this.payments.isEmpty()){
              

            try {
                  PaymentRequest pay = this.payments.poll();

                pay.decideProcessor(payments);
                 String url = "http://payment-processor-" + payments + ":8080/payments";
           String jsonPayload = objectMapper.writeValueAsString(pay);
                HttpClient client = HttpClient.newHttpClient();

                
            HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .header("Content-Type", "application/json")
            .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                
        }

            } catch (Exception e) {

            }
           
    }
           
        
}




public synchronized void payIfNotExist(PaymentRequest pay) {
    boolean isNew = paymentsId.add(pay.getCorrelationId().toString());
    if (!isNew) return;

    try {
        service.inserirPayment(pay);

    } catch (Exception e) {
        // Em caso de falha, remova para permitir retry
        paymentsId.remove(pay.getCorrelationId().toString());
        e.printStackTrace();
        throw e;
    }
}

}

