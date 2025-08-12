package com.rinhaQuarkus.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinhaQuarkus.DTO.PostPaymentDto;
import com.rinhaQuarkus.DTO.ServiceHealthDto;
import com.rinhaQuarkus.enums.Processor;
import com.rinhaQuarkus.jdbc.api.DataService;
import com.rinhaQuarkus.model.PaymentRequest;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.microprofile.config.inject.ConfigProperty;




@ApplicationScoped
public class CacheController {

    private Map<String , CacheHealth> cache = new ConcurrentHashMap<>();

    final Queue<PaymentRequest> payments = new ConcurrentLinkedQueue<>();

    private final Set<String> paymentsId = ConcurrentHashMap.newKeySet();

    private static final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    @ConfigProperty(name = "payment.processor.default.url")
    String paymentProcessorDefaultUrl;

    @Inject
    @ConfigProperty(name = "payment.processor.fallback.url")
    String paymentProcessorFallbackUrl;

    @Inject
    DataService service;

  



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



    private ServiceHealthDto callHeathCheck(){
        try{
                String url = ( "http://payment-processor-default:8080/payments/service-health");

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode()  <= 299){
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
       // System.out.println("Cache: "+health.failing());
       
        long start = System.currentTimeMillis();
        Instant now = Instant.now();
        pay.setRequest_at(now);
        pay.setProcessor(Processor.DEFAULT);
      //  doPostPaymentsDefault(pay, false).thenAccept(sucess ->{
       //     if(sucess){
       //         service.inserirPayment(pay);
       //     }else{
       //         System.out.println("erro na hora de ");
       //     }
       // });

        boolean sucess = doPostPaymentsDefaultTeste(pay, false);

        if(sucess){
            service.inserirPayment(pay);
            long duration = System.currentTimeMillis() - start;
            System.out.println("Requisição levou: " + duration + "ms");

        }else{
            System.out.println("Erro");
        }
        
    

    }


     public boolean doPostPaymentsDefaultTeste(PaymentRequest pay , boolean fallback){

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

                     HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    int status = response.statusCode();
                    if(status >=200 & status <=299){
                        return true;
                    }
                        return false;
                    



            } catch (Exception e) {
               
                
              // throw new RuntimeException("Erro na chamada HTTP do post", e);
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

    public CompletableFuture<Void> doPostPaymentsFallback(PaymentRequest pay){

        String url = "http://payment-processor-fallback:8080/payments";
        try {
            PostPaymentDto dto = new PostPaymentDto(pay.getCorrelationId().toString(),pay.getAmount(),pay.getRequest_at().toString());

            String jsonPayload = objectMapper.writeValueAsString(dto);
           // System.out.println(" PaymentRequest em JSON: default " + jsonPayload);



            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .build();
        
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response ->{
                        if(response.statusCode() >= 200 && response.statusCode() <= 299){
                            //existPayInQueu(pay);
                            //service.inserirPayment(pay);
                        };
                    });



        } catch (Exception e) {
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
           // throw new RuntimeException("Erro na chamada HTTP do post fallbakc", e);

        }


    }


public void existPayInQueu(PaymentRequest pay){
    if(!paymentsId.contains(pay.getCorrelationId().toString())){
         payments.add(pay);
    }  
        paymentsId.add(pay.getCorrelationId().toString());
      
}



public synchronized void payIfNotExist(PaymentRequest pay) {
    boolean isNew = paymentsId.add(pay.getCorrelationId().toString());
    if (!isNew) return;

    try {
        service.inserirVariosPayment(payments);

    } catch (Exception e) {
        // Em caso de falha, remova para permitir retry
        paymentsId.remove(pay.getCorrelationId().toString());
        e.printStackTrace();
        throw e;
    }
}


public synchronized void flushPayments(){
    if(payments.isEmpty()) return;

    Queue<PaymentRequest> toInsert = new ConcurrentLinkedQueue<>();
    PaymentRequest p;
    while((p = payments.poll()) != null){
        toInsert.add(p);
    }
    service.inserirVariosPayment(toInsert);
}

}

