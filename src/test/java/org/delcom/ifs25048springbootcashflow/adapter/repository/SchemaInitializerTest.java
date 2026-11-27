package org.delcom.ifs25048springbootcashflow.adapter.repository;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SchemaInitializerTest {

    @Test
    void testInitialize() throws Exception {

        JdbcDataSource source = new JdbcDataSource();

        source.setURL(
            "jdbc:h2:mem:" + UUID.randomUUID()
            + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        );

        source.setUser("sa");
        source.setPassword("");

        SchemaInitializer.initialize(source);

        // Memastikan inisialisasi ulang tidak gagal.
        SchemaInitializer.initialize(source);

        try (
            Connection connection = source.getConnection();
            Statement statement = connection.createStatement()
        ) {

            try (var result = statement.executeQuery(
                "SELECT COUNT(*) FROM users"
            )) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }

            try (var result = statement.executeQuery(
                "SELECT COUNT(*) FROM transactions"
            )) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
        }
    }

    @Test
    void testInvalidConnection() {

        JdbcDataSource source = new JdbcDataSource();

        source.setURL("jdbc:invalid:test");

        assertThrows(
            IllegalStateException.class,
            () -> SchemaInitializer.initialize(source)
        );
    }
}