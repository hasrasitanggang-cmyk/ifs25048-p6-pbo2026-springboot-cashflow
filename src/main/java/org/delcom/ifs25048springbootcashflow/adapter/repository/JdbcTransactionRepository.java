package org.delcom.ifs25048springbootcashflow.adapter.repository;

import org.delcom.ifs25048springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25048springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25048springbootcashflow.domain.repository.ITransactionRepository;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class JdbcTransactionRepository
        implements ITransactionRepository {

    private final DataSource dataSource;

    public JdbcTransactionRepository(
            DataSource dataSource
    ) {
        this.dataSource = dataSource;
    }

    private static final String SELECT_COLUMNS = """
        SELECT id,
               user_id,
               description,
               amount,
               type,
               proof_path,
               created_at
        FROM transactions
        """;

    @Override
    public List<Transaction> findAll() {

        String sql = SELECT_COLUMNS
                + " ORDER BY created_at DESC, id DESC";

        return query(sql);
    }

    @Override
    public List<Transaction> findByUserId(
            Integer userId
    ) {

        String sql = SELECT_COLUMNS
                + " WHERE user_id = ?"
                + " ORDER BY created_at DESC, id DESC";

        return query(sql, userId);
    }

    @Override
    public Optional<Transaction> findById(
            Integer id
    ) {

        String sql = SELECT_COLUMNS
                + " WHERE id = ?";

        return query(sql, id)
                .stream()
                .findFirst();
    }

    @Override
    public Transaction save(Transaction entity) {

        String sql = """
            INSERT INTO transactions (
                user_id,
                description,
                amount,
                type,
                proof_path,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        LocalDateTime createdAt =
                entity.getCreatedAt() == null
                    ? LocalDateTime.now()
                    : entity.getCreatedAt();

        try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(
                        sql,
                        new String[]{"id"}
                    )
        ) {

            statement.setInt(
                1,
                entity.getUserId()
            );

            statement.setString(
                2,
                entity.getDescription()
            );

            statement.setLong(
                3,
                entity.getAmount()
            );

            statement.setString(
                4,
                entity.getType().name()
            );

            statement.setString(
                5,
                entity.getProofPath()
            );

            statement.setTimestamp(
                6,
                Timestamp.valueOf(createdAt)
            );

            statement.executeUpdate();

            try (
                ResultSet keys =
                    statement.getGeneratedKeys()
            ) {

                if (!keys.next()) {

                    throw new IllegalStateException(
                        "Gagal mendapatkan ID transaksi baru"
                    );
                }

                entity.setId(keys.getInt(1));

                entity.setCreatedAt(createdAt);

                return entity;
            }

        } catch (SQLException ex) {

            throw new IllegalStateException(
                "Gagal menyimpan transaksi",
                ex
            );
        }
    }

    @Override
    public void update(Transaction entity) {

        String sql = """
            UPDATE transactions
            SET description = ?,
                amount = ?,
                type = ?,
                proof_path = ?
            WHERE id = ?
            """;

        try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(
                1,
                entity.getDescription()
            );

            statement.setLong(
                2,
                entity.getAmount()
            );

            statement.setString(
                3,
                entity.getType().name()
            );

            statement.setString(
                4,
                entity.getProofPath()
            );

            statement.setInt(
                5,
                entity.getId()
            );

            statement.executeUpdate();

        } catch (SQLException ex) {

            throw new IllegalStateException(
                "Gagal memperbarui transaksi",
                ex
            );
        }
    }

    @Override
    public boolean deleteById(Integer id) {

        String sql = """
            DELETE FROM transactions
            WHERE id = ?
            """;

        try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setInt(1, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException ex) {

            throw new IllegalStateException(
                "Gagal menghapus transaksi",
                ex
            );
        }
    }

    @Override
    public List<Transaction> findBy(
            Predicate<Transaction> predicate
    ) {

        return findAll()
                .stream()
                .filter(predicate)
                .toList();
    }

    private List<Transaction> query(
            String sql,
            Object... parameters
    ) {

        List<Transaction> transactions =
                new ArrayList<>();

        try (
            Connection connection =
                    dataSource.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            for (int i = 0;
                 i < parameters.length;
                 i++) {

                statement.setObject(
                    i + 1,
                    parameters[i]
                );
            }

            try (
                ResultSet resultSet =
                    statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    transactions.add(
                        mapRow(resultSet)
                    );
                }
            }

            return transactions;

        } catch (SQLException ex) {

            throw new IllegalStateException(
                "Gagal membaca data transaksi",
                ex
            );
        }
    }

    private static Transaction mapRow(
            ResultSet resultSet
    ) throws SQLException {

        return new Transaction(

            resultSet.getInt("id"),

            resultSet.getInt("user_id"),

            resultSet.getString("description"),

            resultSet.getLong("amount"),

            TransactionType.valueOf(
                resultSet.getString("type")
            ),

            resultSet.getString("proof_path"),

            resultSet.getTimestamp("created_at")
                    .toLocalDateTime()
        );
    }
}