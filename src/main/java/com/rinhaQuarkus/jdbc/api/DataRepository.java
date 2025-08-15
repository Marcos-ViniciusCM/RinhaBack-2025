package com.rinhaQuarkus.jdbc.api;

import com.rinhaQuarkus.model.PaymentRequest;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DataRepository implements PanacheRepository<PaymentRequest> {


    public String sumAmount(Instant from , Instant to){


        Object[] result = (Object[]) getEntityManager().createNativeQuery("""
                SELECT
                COUNT(*) FILTER (WHERE processor = 'default')  AS total_default,
                COALESCE(SUM(amount) FILTER (WHERE processor = 'default'), 0) AS amount_default,
                COUNT(*) FILTER (WHERE processor = 'fallback') AS total_fallback,
                COALESCE(SUM(amount) FILTER (WHERE processor = 'fallback'), 0) AS amount_fallback
                FROM payments
                WHERE requested_at BETWEEN :from AND :to
                """)
                .setParameter("from",from)
                .setParameter("to",to)
                .getSingleResult();

        int totalDefault = (int) result[0] ;
        BigDecimal amounDefault = (BigDecimal) result[1];
        int totalFallback = (int) result[2] ;
        BigDecimal amountFallback = (BigDecimal) result[3];

        return String.format("""
                {
          "default": {
            "totalRequests": %d,
            "totalAmount": %s
          },
          "fallback": {
            "totalRequests": %d,
            "totalAmount": %s
          }
        } 
                """,
         totalDefault,amounDefault
        ,totalFallback , amountFallback);
    }

    public Optional<PaymentRequest> findByCorrelationId(UUID correlationId){
        return find("correlationId",correlationId).firstResultOptional();
    }




    @Transactional
    public PaymentRequest save(PaymentRequest pay){
        Optional<PaymentRequest> existPay = findByCorrelationId(pay.getCorrelationId());

        if(existPay.isPresent()){
            PaymentRequest newPay = existPay.get();
            newPay.setProcessor(pay.getProcessor());
            newPay.setAmount(pay.getAmount());
            persist(newPay);
            return newPay;

        }else {
            persist(pay);
            return pay;

        }
    }


}
