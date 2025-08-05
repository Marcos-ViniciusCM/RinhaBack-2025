package com.rinhaQuarkus.jdbc.api;

import com.rinhaQuarkus.DTO.DefaultSumaryDto;
import com.rinhaQuarkus.DTO.FallbackSumaryDto;
import com.rinhaQuarkus.DTO.PaymentsSumaryDto;
import com.rinhaQuarkus.enums.Processor;
import com.rinhaQuarkus.model.PaymentRequest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
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


    public void inserirPayment(PaymentRequest payment) {
        // Validação completa
        if (payment.getCorrelationId() == null ||
                payment.getAmount() == null ||
                payment.getProcessor() == null) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios: " + payment);
        }

        String sql = "INSERT INTO payments(correlationId, amount, processor, requested_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            payment.setProcessor(Processor.FALLBACK);

            stmt.setObject(1, payment.getCorrelationId());
            stmt.setBigDecimal(2, payment.getAmount());
            stmt.setString(3, payment.getProcessor().name().toLowerCase());
            stmt.setTimestamp(4, Timestamp.from(payment.getRequest_at()));

            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao inserir pagamento", e);
        }
    }

}
