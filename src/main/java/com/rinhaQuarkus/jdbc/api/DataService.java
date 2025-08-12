package com.rinhaQuarkus.jdbc.api;

import com.rinhaQuarkus.model.PaymentRequest;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Queue;

@ApplicationScoped
public class DataService {

    @Inject
    AgroalDataSource dataSource;


    public String pegarPayments(Instant from , Instant to ) {

        String sql = """
                SELECT
                COUNT(*) FILTER (WHERE processor = 'default')  AS total_default,
                COALESCE(SUM(amount) FILTER (WHERE processor = 'default'), 0) AS amount_default,
                COUNT(*) FILTER (WHERE processor = 'fallback') AS total_fallback,
                COALESCE(SUM(amount) FILTER (WHERE processor = 'fallback'), 0) AS amount_fallback
                FROM payments
                WHERE requested_at BETWEEN ? AND ?
        """;
        try(
            Connection conn = dataSource.getConnection();
           PreparedStatement statement = conn.prepareStatement(sql);

        ){
            statement.setTimestamp(1 ,Timestamp.from(from));
            statement.setTimestamp(2 ,Timestamp.from(to));


            try(ResultSet rs = statement.executeQuery()){
                if(rs.next()){
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
                            rs.getInt("total_default"),
                            rs.getBigDecimal("amount_default").toPlainString(),
                            rs.getInt("total_fallback"),
                            rs.getBigDecimal("amount_fallback").toPlainString()
                    );
                }
            }


        } catch (SQLException e) {
            System.out.println("erro gerar reusmo service");
            throw new RuntimeException(e);
        }
        return "{}";
    }


    public void inserirPayment(PaymentRequest payment) {
        // Validação completa
        if (payment.getCorrelationId() == null ||
                payment.getAmount() == null ||
                payment.getProcessor() == null) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios: " + payment);
        }

        String sql = "INSERT INTO payments(correlationId, amount, processor, requested_at) VALUES (?, ?, ?, ?)";
        //System.out.println(" Url Conection: " + dataSource.getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {


            stmt.setObject(1, payment.getCorrelationId());
            stmt.setBigDecimal(2, payment.getAmount());
            stmt.setString(3, payment.getProcessor().name().toLowerCase());
            stmt.setTimestamp(4, Timestamp.from(payment.getRequest_at()));

            stmt.executeUpdate();
            //conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao inserir pagamento dados pay"+ payment.toString(), e );
        }
    }

    public void inserirVariosPayment(Queue<PaymentRequest> payments) {


        String sql = "INSERT INTO payments(correlationId, amount, processor, requested_at) VALUES (?, ?, ?, ?)";
        //System.out.println(" Url Conection: " + dataSource.getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);

            for(PaymentRequest payment : payments){
                stmt.setObject(1, payment.getCorrelationId());
                stmt.setBigDecimal(2, payment.getAmount());
                stmt.setString(3, payment.getProcessor().name().toLowerCase());
                stmt.setTimestamp(4, Timestamp.from(payment.getRequest_at()));

            }


            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            //throw new RuntimeException("Falha ao inserir pagamento dados pay"+ payment.toString(), e );
        }
    }



    public void truncarPayment() {
        String sql = "TRUNCATE TABLE payments";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {


            stmt.executeUpdate();
            //conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao apagar database", e );
        }
    }

}
