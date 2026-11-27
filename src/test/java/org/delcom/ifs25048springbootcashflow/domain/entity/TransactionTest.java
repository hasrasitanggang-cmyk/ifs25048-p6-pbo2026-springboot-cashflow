package org.delcom.ifs25048springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testEmptyConstructor() {
        Transaction transaction = new Transaction();

        assertNull(transaction.getId());
        assertNull(transaction.getUserId());
        assertNull(transaction.getDescription());
        assertEquals(0L, transaction.getAmount());
        assertNull(transaction.getType());
        assertNull(transaction.getProofPath());
        assertNull(transaction.getCreatedAt());
    }

    @Test
    void testFullConstructor() {

        LocalDateTime date =
            LocalDateTime.of(2026, 11, 26, 10, 0);

        Transaction transaction = new Transaction(
            1,
            2,
            "Uang bulanan",
            1000000L,
            TransactionType.INCOME,
            "proofs/test.png",
            date
        );

        assertEquals(1, transaction.getId());
        assertEquals(2, transaction.getUserId());
        assertEquals("Uang bulanan", transaction.getDescription());
        assertEquals(1000000L, transaction.getAmount());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals("proofs/test.png", transaction.getProofPath());
        assertEquals(date, transaction.getCreatedAt());
    }

    @Test
    void testNewTransaction() {

        Transaction transaction = new Transaction(
            1,
            "Makan",
            25000L,
            TransactionType.EXPENSE,
            null
        );

        assertNull(transaction.getId());
        assertEquals(1, transaction.getUserId());
        assertEquals("Makan", transaction.getDescription());
        assertEquals(25000L, transaction.getAmount());
        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void testSetters() {

        Transaction transaction = new Transaction();

        LocalDateTime date = LocalDateTime.now();

        transaction.setId(5);
        transaction.setUserId(3);
        transaction.setDescription("Transportasi");
        transaction.setAmount(15000L);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setProofPath("proofs/image.jpg");
        transaction.setCreatedAt(date);

        assertEquals(5, transaction.getId());
        assertEquals(3, transaction.getUserId());
        assertEquals("Transportasi", transaction.getDescription());
        assertEquals(15000L, transaction.getAmount());
        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals("proofs/image.jpg", transaction.getProofPath());
        assertEquals(date, transaction.getCreatedAt());
    }
}