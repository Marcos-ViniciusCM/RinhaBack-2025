package com.rinhaQuarkus.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinhaQuarkus.DTO.ServiceHealthDto;
import com.rinhaQuarkus.jdbc.api.DataService;
import com.rinhaQuarkus.model.PaymentRequest;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheController {

    private Map<String , CacheHealth> cache = new ConcurrentHashMap<>();

    ArrayList<PaymentRequest> payments = new ArrayList<>();

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
        try(CloseableHttpClient client = HttpClients.createDefault()){
            HttpGet request = new HttpGet(url);
            try(CloseableHttpResponse response = client.execute(request)){
                int status = response.getStatusLine().getStatusCode();

                if(status <= 200 && status >= 299){
                    return new ServiceHealthDto(true , 9999 );
                }

                String json = EntityUtils.toString(response.getEntity());
                return objectMapper.readValue(json, ServiceHealthDto.class);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void decideWich(PaymentRequest pay){
        ServiceHealthDto health = checkCache("default");
        ServiceHealthDto healthFallback = checkCache("fallback");
        if(health.minResponseTime() > 10 && !health.failling()){
            payments.add(pay);
        }
        if(health.minResponseTime() <= 10 && !health.failling()){
            //fazer o post no default se o valor for menor que 10 ms;
            doPostPayments("default",pay);
        }else if(health.failling() && payments.size() > 10){
            // seo estiver falhando e o vetor maior que 10 fazer no fallback
            doPostPaymentsArray("fallback");
        }


    }


    public void doPostPayments(String payments ,PaymentRequest pay){
        String url = "http://payment-processor-"+payments+":8080/payments";
        String jsonPayload = "{"
                + "\"correlationId\": \"" + pay.getId() + "\", "
                + "\"amount\": " + pay.getAmount()
                + "}";

        try(CloseableHttpClient cliente = HttpClients.createDefault()){
            HttpPost request = new HttpPost(url);
            StringEntity requestEntity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            request.setEntity(requestEntity);
            try(CloseableHttpResponse response = cliente.execute(request)){
                int status = response.getStatusLine().getStatusCode();

                if (status >= 200 && status <= 299) {
                    service.inserirPayment(pay);
                }

            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void doPostPaymentsArray(String payments){
        for(PaymentRequest pay: this.payments) {
            String url = "http://payment-processor-" + payments + ":8080/payments";
            String jsonPayload = "{"
                    + "\"correlationId\": \"" + pay.getId() + "\", "
                    + "\"amount\": " + pay.getAmount()
                    + "}";

            try (CloseableHttpClient cliente = HttpClients.createDefault()) {
                HttpPost request = new HttpPost(url);
                StringEntity requestEntity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
                request.setEntity(requestEntity);
                try (CloseableHttpResponse response = cliente.execute(request)) {
                    int status = response.getStatusLine().getStatusCode();

                    if (status >= 200 && status <= 299) {
                        service.inserirPayment(pay);
                    }

                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.payments.clear();
    }
}
