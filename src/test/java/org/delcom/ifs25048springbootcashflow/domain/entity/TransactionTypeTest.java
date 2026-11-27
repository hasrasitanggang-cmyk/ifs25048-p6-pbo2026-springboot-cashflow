package org.delcom.ifs25048springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTypeTest {

    @Test
    void testLabels() {

        assertEquals(
            "Pemasukan",
            TransactionType.INCOME.label()
        );

        assertEquals(
            "Pengeluaran",
            TransactionType.EXPENSE.label()
        );
    }

    @Test
    void testValues() {

        assertEquals(2, TransactionType.values().length);

        assertEquals(
            TransactionType.INCOME,
            TransactionType.valueOf("INCOME")
        );

        assertEquals(
            TransactionType.EXPENSE,
            TransactionType.valueOf("EXPENSE")
        );
    }
}