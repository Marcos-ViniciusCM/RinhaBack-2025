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
        FROM payments
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
                int request = rs.getInt("totalRequests");
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
        if (payments.getCorrelationId() == null || payments.getAmount() == null || payments.getProcessor() == null) {
            throw new IllegalArgumentException("Invalid payment data: CorrelationId, Amount, and Processor must not be null.");
        }
        Instant now = Instant.now();
        payments.setRequest_at(now);
        String sql = """
                    INSERT INTO payments( correlationId , amount , processor , requested_at ) 
                    VALUES( ? , ? , ? , ?);
                    """;
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement statement = conn.prepareStatement(sql);
        ){

            statement.setObject(1,payments.getCorrelationId());
            statement.setBigDecimal(2 , payments.getAmount());
            statement.setObject(3 , payments.getProcessor().name().toLowerCase());
            statement.setTimestamp(4 ,Timestamp.from(now));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    public void cretateTable(){
        Instant now = Instant.now();
        String sql = """
                CREATE TABLE IF NOT EXISTS payments (
                                           correlationId UUID PRIMARY KEY NOT NULL UNIQUE,
                                           amount NUMERIC(10,2) NOT NULL,
                                           processor VARCHAR(10) NOT NULL CHECK (processor IN ('default', 'fallback')),
                                           requested_at TIMESTAMP NOT NULL
                                       );
                    """;
        try(
                Connection conn = dataSource.getConnection();
                PreparedStatement statement = conn.prepareStatement(sql);
        ){
            // Executando a query
            statement.executeUpdate();  // Usamos executeUpdate() para comandos DDL como CREATE TABLE
            System.out.println("Tabela 'payments' criada ou já existente.");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
