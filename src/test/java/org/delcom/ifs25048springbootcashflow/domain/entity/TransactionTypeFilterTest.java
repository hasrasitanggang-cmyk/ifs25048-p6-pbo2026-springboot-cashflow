package org.delcom.ifs25048springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTypeFilterTest {

    @Test
    void testAllFilter() {

        assertTrue(
            TransactionTypeFilter.ALL.matches(
                TransactionType.INCOME
            )
        );

        assertTrue(
            TransactionTypeFilter.ALL.matches(
                TransactionType.EXPENSE
            )
        );
    }

    @Test
    void testIncomeFilter() {

        assertTrue(
            TransactionTypeFilter.INCOME.matches(
                TransactionType.INCOME
            )
        );

        assertFalse(
            TransactionTypeFilter.INCOME.matches(
                TransactionType.EXPENSE
            )
        );
    }

    @Test
    void testExpenseFilter() {

        assertTrue(
            TransactionTypeFilter.EXPENSE.matches(
                TransactionType.EXPENSE
            )
        );

        assertFalse(
            TransactionTypeFilter.EXPENSE.matches(
                TransactionType.INCOME
            )
        );
    }

    @Test
    void testLabels() {

        assertEquals(
            "Semua tipe",
            TransactionTypeFilter.ALL.label()
        );

        assertEquals(
            "Pemasukan",
            TransactionTypeFilter.INCOME.label()
        );

        assertEquals(
            "Pengeluaran",
            TransactionTypeFilter.EXPENSE.label()
        );
    }
}