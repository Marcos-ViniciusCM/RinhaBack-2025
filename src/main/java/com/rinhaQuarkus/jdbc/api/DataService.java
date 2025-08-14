package com.rinhaQuarkus.jdbc.api;

import com.rinhaQuarkus.model.PaymentRequest;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
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

    @Inject
    ReactivePgClient client;


     public void warmUp() throws SQLException {
       try( Connection con = dataSource.getConnection();
           PreparedStatement ps = con.prepareStatement("SELECT processor, COUNT(*) AS totalRequests," +
                   " SUM(amount) AS totalAmount FROM payments WHERE requested_at BETWEEN ? AND ? GROUP BY processor")){
           ps.setTimestamp(1, Timestamp.from(Instant.now().minusSeconds(3600)));
           ps.setTimestamp(2, Timestamp.from(Instant.now()));
           try (ResultSet rs = ps.executeQuery()) {
               while (rs.next()) {
                   // só para iterar e carregar
                   rs.getString("processor");
                   rs.getInt("totalRequests");
                   rs.getBigDecimal("totalAmount");
               }
           }
       }
    }

     public Uni<Void> inserirPayment(PaymentRequest payment) {
        String sql = "INSERT INTO payments(correlationId, amount, processor, requested_at) VALUES ($1, $2, $3, $4)";
        return client.preparedQuery(sql)
            .execute(Tuple.of(payment.getCorrelationId(), payment.getAmount(), payment.getProcessor().name(), payment.getRequest_at()))
            .replaceWithVoid();
    }


    public String pegarPayments(Instant from , Instant to ) {
        long start = System.currentTimeMillis();
        Instant now = Instant.now();
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
            long duration = System.currentTimeMillis() - start;
            System.out.println("Banco demou pra trazer o get: " + duration + "ms");
            }


        } catch (SQLException e) {
            System.out.println("erro gerar reusmo service");
            throw new RuntimeException(e);
        }
        return "{}";
    }


    public boolean verificarDuplicada(PaymentRequest pay){
        String sql = "SELECT 1 FROM payments WHERE correlationId = ? LIMIT 1";
        try(Connection conn = dataSource.getConnection();
        PreparedStatement statement = conn.prepareStatement(sql)){
            statement.setObject(1, pay.getCorrelationId());
            try(ResultSet rs = statement.executeQuery()){
               return rs.next();
            }
        }catch (Exception e) {
          
        }
        return false;
    }


    public void inserirPayment2(PaymentRequest payment) {
        long start = System.currentTimeMillis();

        // Validação completa
        if (payment.getCorrelationId() == null ||
                payment.getAmount() == null ||
                payment.getProcessor() == null) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios: " + payment);
        }


        
      

        String sql = "INSERT INTO payments(correlationId, amount, processor, requested_at) VALUES (?, ?, ?, ?)";
       // System.out.println(" Url Conection: " + dataSource.getConfiguration().connectionPoolConfiguration().connectionFactoryConfiguration().jdbcUrl());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);

            stmt.setObject(1, payment.getCorrelationId());
            stmt.setBigDecimal(2, payment.getAmount());
            stmt.setString(3, payment.getProcessor().name().toLowerCase());
            stmt.setTimestamp(4, Timestamp.from(payment.getRequest_at()));

            stmt.executeUpdate();
            conn.commit();
            long duration = System.currentTimeMillis() - start;
            System.out.println("Inserção demorou: " + duration + "ms");

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
                stmt.addBatch();
            }


            stmt.executeBatch();
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
