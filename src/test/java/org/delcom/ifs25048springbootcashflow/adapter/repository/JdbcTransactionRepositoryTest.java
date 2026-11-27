
package org.delcom.ifs25048springbootcashflow.adapter.repository;

import org.delcom.ifs25048springbootcashflow.domain.entity.*;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTransactionRepositoryTest {

    private JdbcDataSource dataSource;
    private JdbcTransactionRepository repository;

    @BeforeEach
    void setUp() throws Exception {

        dataSource = new JdbcDataSource();

        dataSource.setURL(
            "jdbc:h2:mem:" + UUID.randomUUID()
            + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        );

        dataSource.setUser("sa");
        dataSource.setPassword("");

        SchemaInitializer.initialize(dataSource);

        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()
        ) {

            statement.executeUpdate("""
                INSERT INTO users
                (id, username, password_hash, full_name, role)
                VALUES
                (1, 'user1', 'hash1', 'User One', 'USER'),
                (2, 'user2', 'hash2', 'User Two', 'USER')
            """);
        }

        repository = new JdbcTransactionRepository(dataSource);
    }

    private Transaction create(
        int userId,
        String description,
        long amount,
        TransactionType type
    ) {

        return new Transaction(
            userId,
            description,
            amount,
            type,
            null
        );
    }

    // =====================================================
    // HELPER: SIMULASI DATABASE TANPA GENERATED KEY
    // =====================================================

    private DataSource dataSourceWithoutGeneratedKeys() {

        return (DataSource) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{DataSource.class},
            (proxy, method, args) -> {

                if (method.getName().equals("getConnection")
                        && (args == null || args.length == 0)) {

                    Connection realConnection =
                        dataSource.getConnection();

                    return Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (connectionProxy, connectionMethod, connectionArgs) -> {

                            Object result = invoke(
                                realConnection,
                                connectionMethod,
                                connectionArgs
                            );

                            if (connectionMethod.getName().equals("prepareStatement")
                                    && result instanceof PreparedStatement realStatement) {

                                return Proxy.newProxyInstance(
                                    getClass().getClassLoader(),
                                    new Class<?>[]{PreparedStatement.class},
                                    (statementProxy, statementMethod, statementArgs) -> {

                                        if (statementMethod.getName().equals("getGeneratedKeys")) {
                                            return emptyResultSet();
                                        }

                                        return invoke(
                                            realStatement,
                                            statementMethod,
                                            statementArgs
                                        );
                                    }
                                );
                            }

                            return result;
                        }
                    );
                }

                return invoke(dataSource, method, args);
            }
        );
    }

    private static Object invoke(
        Object target,
        Method method,
        Object[] args
    ) throws Throwable {

        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static ResultSet emptyResultSet() {

        return (ResultSet) Proxy.newProxyInstance(
            JdbcTransactionRepositoryTest.class.getClassLoader(),
            new Class<?>[]{ResultSet.class},
            (proxy, method, args) -> {

                return switch (method.getName()) {
                    case "next" -> false;
                    case "close" -> null;
                    case "isClosed" -> false;
                    case "toString" -> "Empty Generated Keys";

                    default -> throw new UnsupportedOperationException(
                        "Method tidak didukung: " + method.getName()
                    );
                };
            }
        );
    }

    // =====================================================
    // TEST SAVE
    // =====================================================

    @Test
    void testSaveIncome() {

        Transaction saved = repository.save(
            create(1, "Gaji", 1000000, TransactionType.INCOME)
        );

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Transaction found = repository.findById(
            saved.getId()
        ).orElseThrow();

        assertEquals("Gaji", found.getDescription());
        assertEquals(1000000L, found.getAmount());
        assertEquals(TransactionType.INCOME, found.getType());
    }

    @Test
    void testSaveExpense() {

        Transaction saved = repository.save(
            create(1, "Makan", 25000, TransactionType.EXPENSE)
        );

        assertEquals(TransactionType.EXPENSE, saved.getType());
        assertEquals(25000L, saved.getAmount());
    }

    @Test
    void testSaveWithoutDate() {

        Transaction transaction = create(
            1, "Buku", 75000, TransactionType.EXPENSE
        );

        transaction.setCreatedAt(null);

        Transaction saved = repository.save(transaction);

        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void testSaveWithProof() {

        Transaction transaction = create(
            1, "Belanja", 50000, TransactionType.EXPENSE
        );

        transaction.setProofPath("proofs/test.png");

        Transaction saved = repository.save(transaction);

        assertEquals(
            "proofs/test.png",
            repository.findById(saved.getId())
                .orElseThrow().getProofPath()
        );
    }

    // TEST JALUR GENERATED KEY TIDAK DITEMUKAN

    @Test
    void testSaveWithoutGeneratedKey() {

        JdbcTransactionRepository repositoryWithoutKeys =
            new JdbcTransactionRepository(
                dataSourceWithoutGeneratedKeys()
            );

        Transaction transaction = create(
            1,
            "Test Generated Key",
            1000,
            TransactionType.INCOME
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> repositoryWithoutKeys.save(transaction)
        );

        assertEquals(
            "Gagal mendapatkan ID transaksi baru",
            exception.getMessage()
        );
    }

    // =====================================================
    // TEST FIND
    // =====================================================

    @Test
    void testFindAll() {

        repository.save(
            create(1, "Gaji", 1000000, TransactionType.INCOME)
        );

        repository.save(
            create(1, "Makan", 25000, TransactionType.EXPENSE)
        );

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void testFindByUserId() {

        repository.save(
            create(1, "Gaji", 1000000, TransactionType.INCOME)
        );

        repository.save(
            create(2, "Bonus", 500000, TransactionType.INCOME)
        );

        assertEquals(1, repository.findByUserId(1).size());
        assertEquals(1, repository.findByUserId(2).size());
        assertTrue(repository.findByUserId(999).isEmpty());
    }

    @Test
    void testFindByIdNotFound() {

        assertTrue(repository.findById(999).isEmpty());
    }

    @Test
    void testFindByPredicate() {

        repository.save(
            create(1, "Gaji", 1000000, TransactionType.INCOME)
        );

        repository.save(
            create(1, "Makan", 25000, TransactionType.EXPENSE)
        );

        var result = repository.findBy(
            item -> item.getType() == TransactionType.INCOME
        );

        assertEquals(1, result.size());
        assertEquals("Gaji", result.get(0).getDescription());
    }

    @Test
    void testNewestFirst() {

        Transaction first = create(
            1, "Pertama", 1000, TransactionType.INCOME
        );

        first.setCreatedAt(
            LocalDateTime.of(2026, 11, 25, 10, 0)
        );

        Transaction second = create(
            1, "Kedua", 2000, TransactionType.INCOME
        );

        second.setCreatedAt(
            LocalDateTime.of(2026, 11, 26, 10, 0)
        );

        repository.save(first);
        repository.save(second);

        assertEquals(
            "Kedua",
            repository.findAll().get(0).getDescription()
        );
    }

    // =====================================================
    // TEST UPDATE
    // =====================================================

    @Test
    void testUpdate() {

        Transaction saved = repository.save(
            create(1, "Makan", 25000, TransactionType.EXPENSE)
        );

        saved.setDescription("Makan malam");
        saved.setAmount(35000);
        saved.setProofPath("proofs/new.png");

        repository.update(saved);

        Transaction result = repository.findById(
            saved.getId()
        ).orElseThrow();

        assertEquals("Makan malam", result.getDescription());
        assertEquals(35000L, result.getAmount());
        assertEquals("proofs/new.png", result.getProofPath());
    }

    // =====================================================
    // TEST DELETE
    // =====================================================

    @Test
    void testDelete() {

        Transaction saved = repository.save(
            create(1, "Makan", 25000, TransactionType.EXPENSE)
        );

        assertTrue(repository.deleteById(saved.getId()));
        assertTrue(repository.findById(saved.getId()).isEmpty());
        assertFalse(repository.deleteById(saved.getId()));
    }

    // =====================================================
    // TEST SQL EXCEPTION
    // =====================================================

    @Test
    void testSqlErrors() throws Exception {

        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()
        ) {

            statement.execute("DROP TABLE transactions");
        }

        Transaction transaction = create(
            1, "Test", 1000, TransactionType.INCOME
        );

        transaction.setId(1);

        assertThrows(
            IllegalStateException.class,
            () -> repository.save(transaction)
        );

        assertThrows(
            IllegalStateException.class,
            () -> repository.update(transaction)
        );

        assertThrows(
            IllegalStateException.class,
            () -> repository.findAll()
        );

        assertThrows(
            IllegalStateException.class,
            () -> repository.deleteById(1)
        );
    }
}
