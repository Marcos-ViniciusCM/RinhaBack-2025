package com.rinhaQuarkus.jdbc.api;

import com.rinhaQuarkus.DTO.DefaultSumaryDto;
import com.rinhaQuarkus.DTO.FallbackSumaryDto;
import com.rinhaQuarkus.DTO.PaymentsSumaryDto;
import com.rinhaQuarkus.model.PaymentRequest;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;

@ApplicationScoped
public class DataService {

    @Inject
    AgroalDataSource dataSource;

    public PaymentsSumaryDto pegarPayments(Instant from , Instant to ) {
        String sql = """
        SELECT processor, COUNT(*) AS totalRequests, SUM(amount) AS totalAmount
        FROM paymentRequest
        WHERE requested_at BETWEEN ? AND ?
        GROUP BY processor
        """;
        try(
            Connection conn = dataSource.getConnection();
            PreparedStatement statement = conn.prepareStatement(sql);
        ){
            statement.setTimestamp(1 ,Timestamp.from(from));
            statement.setTimestamp(2 ,Timestamp.from(to));
            ResultSet rs = statement.executeQuery();
            DefaultSumaryDto defaults = new DefaultSumaryDto(0, BigDecimal.ZERO);
            FallbackSumaryDto fallback = new FallbackSumaryDto(0 , BigDecimal.ZERO);


            while  (rs.next()){
                String processor = rs.getString("processor");
                int request = rs.getInt("totalRequest");
                BigDecimal amount = rs.getBigDecimal("totalAmount");

                if("default".equals(processor)){
                    defaults = new DefaultSumaryDto(request , amount);
                }
                if("fallback".equals(processor)){
                    fallback = new FallbackSumaryDto(request, amount);
                }

            }
            PaymentsSumaryDto sumary = new PaymentsSumaryDto(defaults , fallback);
            return sumary;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void inserirPayment(PaymentRequest payments){
        Instant now = Instant.now();
        String sql = """
                    INSERT INTO paymentRequest( id , amount , processor , request_at ) 
                    VALUES( ? , ? , ? , ?);
                    """;
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement statement = conn.prepareStatement(sql);
        ){
            statement.setObject(1,payments.getCorrelationId());
            statement.setBigDecimal(2 , payments.getAmount());
            statement.setObject(3 , payments.getProcessor());
            statement.setTimestamp(4 ,Timestamp.from(now));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
