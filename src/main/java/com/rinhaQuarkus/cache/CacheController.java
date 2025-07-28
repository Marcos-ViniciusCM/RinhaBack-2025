package com.rinhaQuarkus.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinhaQuarkus.DTO.ServiceHealthDto;
import com.rinhaQuarkus.enums.Processor;
import com.rinhaQuarkus.jdbc.api.DataService;
import com.rinhaQuarkus.model.PaymentRequest;
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

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class CacheController {

    private Map<String , CacheHealth> cache = new ConcurrentHashMap<>();

    final Queue<PaymentRequest> payments = new ConcurrentLinkedQueue<>();

    private final Set<String> paymentsId = ConcurrentHashMap.newKeySet();

    private final CloseableHttpClient httpClient;

    public CacheController() {
        this.httpClient = HttpClients.createDefault();
    }



    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    DataService service;



    private ServiceHealthDto checkCache(String key){
        CacheHealth cached = cache.get(key);
        if(cached != null && !cached.isExpired()){
            return cached.getData();
        }
        ServiceHealthDto fresh = callHeathCheck(key);
        cache.put(key, new CacheHealth(fresh , Instant.now().plusSeconds(5)));
        return fresh;
    }

    private ServiceHealthDto callHeathCheck(String processor){
        String url = ( "http://payment-processor-"+processor+":8080/payments/service-health");
        
            HttpGet request = new HttpGet(url);
            try(CloseableHttpResponse response = httpClient.execute(request)){
                int status = response.getStatusLine().getStatusCode();

                if(status < 200 || status > 299){
                    throw new RuntimeException("Service health check failed for processor: " + processor);
                }

                String json = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(json, ServiceHealthDto.class);
            
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void decideWich(PaymentRequest pay){
        ServiceHealthDto health = checkCache("default");
        ServiceHealthDto healthFallback = checkCache("fallback");
        if(health.minResponseTime() > 1000|| health.failing() && healthFallback.minResponseTime() > 1000){
            payments.add(pay);
        }else if(healthFallback.minResponseTime() < 400 || health.minResponseTime() > 600){
            pay.setProcessor(Processor.FALLBACK);
            doPostPayments("fallback",pay);
            service.inserirPayment(pay);
        } else if(health.minResponseTime() <= 500 && !health.failing()){
            //fazer o post no default se o valor for menor que 150 ms;
            pay.setProcessor(Processor.DEFAULT);
            doPostPayments("default",pay);
            service.inserirPayment(pay);
//        }else if(health.failing() && healthFallback.minResponseTime() < 300 && this.payments.size() > 10){
//            // seo estiver falhando e o vetor maior que 10 fazer no fallback
//                doPostPaymentsArray("fallback");
        }
//        else if(health.minResponseTime() < 400 && this.payments.size() > 10){
//            doPostPaymentsArray("default");
//        }


    }


    public void doPostPayments(String payments ,PaymentRequest pay){
        String url = "http://payment-processor-"+payments+":8080/payments";
        String jsonPayload = "{"
                + "\"correlationId\": \"" + pay.getCorrelationId() + "\", "
                + "\"amount\": " + pay.getAmount()
                + "}";

        
            HttpPost request = new HttpPost(url);
            StringEntity requestEntity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            request.setEntity(requestEntity);
            try(CloseableHttpResponse response = httpClient.execute(request)){
                int status = response.getStatusLine().getStatusCode();

                if (status >= 200 && status <= 299) {
                    service.inserirPayment(pay);
                }

            
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void doPostPaymentsArray(String payments){

            while(!this.payments.isEmpty()){
                PaymentRequest pay = this.payments.poll();

                pay.decideProcessor(payments);
                 String url = "http://payment-processor-" + payments + ":8080/payments";
            String jsonPayload = "{"
                    + "\"correlationId\": \"" + pay.getCorrelationId() + "\", "
                    + "\"amount\": " + pay.getAmount()
                    + "}";


                HttpPost request = new HttpPost(url);
                StringEntity requestEntity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
                request.setEntity(requestEntity);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int status = response.getStatusLine().getStatusCode();

                    if (status >= 200 && status <= 299) {

                        service.inserirPayment(pay);
                    }

                
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        
                
            }
           
        
    }


    public void addPayment(PaymentRequest pay){
        if(paymentsId.add(pay.getCorrelationId().toString())){
            payments.add(pay);
        }
    }
}
